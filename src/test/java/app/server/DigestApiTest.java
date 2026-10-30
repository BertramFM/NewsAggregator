package app.server;

import app.config.HibernateTestConfig;
import app.config.TokenSettings;
import app.daos.SecurityDAO;
import app.daos.SubscriptionDAO;
import app.entities.Frequency;
import app.entities.Subscription;
import app.entities.User;
import app.testUtils.DatabaseTestPopulator;
import app.testUtils.DatabaseTestPopulator.SeedData;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class DigestApiTest {

    private static final int PORT = 7780;
    private static final String PASSWORD = "Hemmelig1!";
    private static final TokenSettings TEST_TOKEN_SETTINGS =
            new TokenSettings("test-secret-key-that-is-at-least-32-chars", "test", "3600000");
    private static EntityManagerFactory emf;
    private static Javalin app;
    private static SecurityDAO securityDAO;
    private static SubscriptionDAO subscriptionDAO;
    private SeedData seed;

    @BeforeAll
    static void startServer() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        securityDAO = new SecurityDAO(emf);
        subscriptionDAO = new SubscriptionDAO(emf);
        app = JavalinServer.startServer(emf, PORT, TEST_TOKEN_SETTINGS);
        RestAssured.baseURI = "http://localhost";
    }

    @AfterAll
    static void stopServer() {
        app.stop();
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = PORT;
        seed = DatabaseTestPopulator.populate(emf);
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

    @Test
    void getMine_withoutToken_returns401() {
        given().when().get("/api/digests").then().statusCode(401);
    }

    @Test
    void getMine_subscribedUser_returnsSharedDigest() {
        User anna = securityDAO.createUser("anna@newssite.com", "anna", PASSWORD);
        subscriptionDAO.create(Subscription.builder()
                .user(anna)
                .topic(seed.science())
                .frequency(Frequency.DAILY)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        given()
                .auth().oauth2(login("anna@newssite.com"))
                .when()
                .get("/api/digests")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].topicName", equalTo("science"))
                .body("[0].frequency", equalTo("DAILY"))
                .body("[0].digestText", equalTo("Test-digest"))
                .body("[0].periodStart", startsWith("2026-10-13T00:00"));
    }

    @Test
    void getMine_userWithoutSubscriptions_returnsEmptyList() {
        securityDAO.createUser("anna@newssite.com", "anna", PASSWORD);

        given()
                .auth().oauth2(login("anna@newssite.com"))
                .when()
                .get("/api/digests")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }
}
