package app.server;

import app.config.HibernateTestConfig;
import app.config.TokenSettings;
import app.daos.SecurityDAO;
import app.testUtils.DatabaseTestPopulator;
import app.testUtils.DatabaseTestPopulator.SeedData;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class ArticleApiTest {
    private static final int PORT = 7777;
    private static final String PASSWORD = "Hemmelig1!";
    private static final TokenSettings TEST_TOKEN_SETTINGS =
            new TokenSettings("the-most-secret-key-that-is-at-least-32-chars-long", "test", "3600000");
    private static SecurityDAO securityDAO;
    private static EntityManagerFactory emf;
    private static Javalin app;
    private SeedData seed;

    @BeforeAll
    static void startServer() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        securityDAO = new SecurityDAO(emf);
        app = JavalinServer.startServer(emf, PORT, TEST_TOKEN_SETTINGS);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = PORT;
    }

    @AfterAll
    static void stopServer() {
        app.stop();
    }

    @BeforeEach
    void setUp() {
        seed = DatabaseTestPopulator.populate(emf);
    }

    @Test
    void getArticles_returnsAll_NewestFirst() {
        given()
                .when()
                .get("/api/articles")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].title", equalTo("New chip announced"))
                .body("[0].sourceName", equalTo("The Guardian"))
                .body("[0].topicName", equalTo("technology"))
                .body("[0].publishedAt", startsWith("2026-10-19T12:00"));
    }

    @Test
    void getArticleById_returnsArticle() {
        given()
                .auth().oauth2(userToken())
                .when()
                .get("/api/articles/{id}", seed.article1().getId())
                .then()
                .statusCode(200)
                .body("title", equalTo("Scientists find new planet"))
                .body("topicName", equalTo("science"));
    }

    @Test
    void getArticleById_invalidId_returns400() {
        given()
                .auth().oauth2(userToken())
                .when()
                .get("/api/articles/abc")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("message", containsString("Invalid ID format"));
    }

    @Test
    void getArticleById_unknownId_returns404() {
        given()
                .auth().oauth2(userToken())
                .when()
                .get("/api/articles/999999")
                .then()
                .statusCode(404)
                .body("message", containsString("not found"));
    }

    @Test
    void getArticleById_withoutToken_returns401() {
        given()
                .when()
                .get("/api/articles/{id}", seed.article1().getId())
                .then()
                .statusCode(401);
    }

    private String login(String email) {
        return given()
                .contentType("application/json")
                .body("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

    private String adminToken() {
        securityDAO.createUser("admin@newssite.com", "admin", PASSWORD);
        securityDAO.createRole("ADMIN");
        securityDAO.addUserRole("admin@newssite.com", "ADMIN");
        return login("admin@newssite.com");
    }

    private String userToken() {
        securityDAO.createUser("user@newssite.com", "user", PASSWORD);
        return login("user@newssite.com");
    }

    @Test
    void deleteArticle_removesItAndKeepsDigestLogIntact() {
        Long id = seed.article1().getId();
        String token = adminToken();

        given().auth().oauth2(token).when().delete("/api/articles/{id}", id).then().statusCode(200);
        given().auth().oauth2(token).when().get("/api/articles/{id}", id).then().statusCode(404);
        given().when().get("/api/articles").then().statusCode(200).body("size()", equalTo(1));
    }

    @Test
    void deleteArticle_unknownId_returns404() {
        given().auth().oauth2(adminToken()).when().delete("/api/articles/999999").then().statusCode(404);
    }

    @Test
    void deleteArticle_withoutToken_returns401() {
        given().when().delete("/api/articles/{id}", seed.article1().getId())
                .then().statusCode(401).body("status", equalTo(401));
    }

    @Test
    void deleteArticle_withInvalidToken_returns401() {
        given().auth().oauth2("notMyToken").when().delete("/api/articles/{id}", seed.article1().getId())
                .then().statusCode(401);
    }

    @Test
    void deleteArticle_asUser_returns403() {
        given().auth().oauth2(userToken()).when().delete("/api/articles/{id}", seed.article1().getId())
                .then().statusCode(403).body("status", equalTo(403));
    }

    @Test
    void getArticles_withoutToken_isOpen() {
        given().when().get("/api/articles").then().statusCode(200);
    }
}