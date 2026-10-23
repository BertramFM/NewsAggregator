package app.server;

import app.config.HibernateTestConfig;
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
    private static EntityManagerFactory emf;
    private static Javalin app;
    private SeedData seed;

    @BeforeAll
    static void startServer() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        app = JavalinServer.startServer(emf, PORT);
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
                .when()
                .get("/api/articles/999999")
                .then()
                .statusCode(404)
                .body("message", containsString("not found"));
    }

    @Test
    void deleteArticle_removesItAndKeepsDigestLogIntact() {
        Long id = seed.article1().getId();

        given().when().delete("/api/articles/{id}", id).then().statusCode(200);
        given().when().get("/api/articles/{id}", id).then().statusCode(404);
        given().when().get("/api/articles").then().statusCode(200).body("size()", equalTo(1));
    }

    @Test
    void deleteArticle_unknownId_returns404() {
        given().when().delete("/api/articles/999999").then().statusCode(404);
    }
}