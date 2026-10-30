package app.server;

import app.config.HibernateTestConfig;
import app.config.TokenSettings;
import app.testUtils.DatabaseTestPopulator;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class SecurityApiTest {
    private static final int PORT = 7778;
    private static final TokenSettings TEST_TOKEN_SETTINGS =
            new TokenSettings("test-secret-key-that-is-at-least-32-chars", "test", "3600000");
    private static EntityManagerFactory emf;
    private static Javalin app;

    @BeforeAll
    static void startServer() {
        emf = HibernateTestConfig.getEntityManagerFactory();
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
        RestAssured.port = PORT;
        DatabaseTestPopulator.populate(emf);
    }

    private void register(String email, String userName, String password, int expectedStatus) {
        given()
                .contentType("application/json")
                .body("{\"email\": \"" + email + "\", \"userName\": \"" + userName
                        + "\", \"password\": \"" + password + "\"}")
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    void register_returns201WithTokenAndNormalizedEmail() {
        given()
                .contentType("application/json")
                .body("{\"email\": \" Anna@NewsSite.com \", \"userName\": \"anna\", \"password\": \"Hemmelig1!\"}")
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("token", not(emptyOrNullString()))
                .body("email", equalTo("anna@newssite.com"));
    }

    @Test
    void register_weakPassword_returns400WithDanishMessage() {
        given()
                .contentType("application/json")
                .body("{\"email\": \"anna@newssite.com\", \"userName\": \"anna\", \"password\": \"kort\"}")
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .body("message", equalTo("Password skal være mindst 8 tegn"));
    }

    @Test
    void register_duplicateEmail_returns409() {
        register("anna@newssite.com", "anna", "Hemmelig1!", 201);
        register("anna@newssite.com", "anna2", "Hemmelig1!", 409);
    }

    @Test
    void register_invalidJson_returns400() {
        given()
                .contentType("application/json")
                .body("not json")
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    void login_correctCredentials_returns200WithToken() {
        register("anna@newssite.com", "anna", "Hemmelig1!", 201);

        given()
                .contentType("application/json")
                .body("{\"email\": \"anna@newssite.com\", \"password\": \"Hemmelig1!\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", not(emptyOrNullString()))
                .body("email", equalTo("anna@newssite.com"));
    }

    @Test
    void login_wrongPassword_returns401() {
        register("anna@newssite.com", "anna", "Hemmelig1!", 201);

        given()
                .contentType("application/json")
                .body("{\"email\": \"anna@newssite.com\", \"password\": \"Forkert1!\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("message", equalTo("Forkert email eller password"));
    }
}
