package app.server;

import app.config.HibernateTestConfig;
import app.config.TokenSettings;
import app.daos.SecurityDAO;
import app.testUtils.DatabaseTestPopulator;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class SubscriptionApiTest {

    private static final int PORT = 6666;
    private static final String PASSWORD = "Hemmelig1!";
    private static final TokenSettings TEST_TOKEN_SETTINGS =
            new TokenSettings("inmwnf89wenfunvpuew90qw8benfunjeifwnejf89f8wehf7n8e9wbn", "test", "3600000");
    private static EntityManagerFactory emf;
    private static Javalin app;
    private static SecurityDAO securityDAO;

    @BeforeAll
    static void startServer() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        securityDAO = new SecurityDAO(emf);
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
        DatabaseTestPopulator.populate(emf);
    }

    private String tokenFor(String email, String userName) {
        securityDAO.createUser(email, userName, PASSWORD);
        return given()
                .contentType("application/json")
                .body("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

    private void subscribe(String token, String topicName, String frequency, int expectedStatus) {
        given()
                .auth().oauth2(token)
                .contentType("application/json")
                .body("{\"topicName\": \"" + topicName + "\", \"frequency\": \"" + frequency + "\"}")
                .when()
                .post("/api/subscriptions")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    void create_returns201WithSubscription() {
        String token = tokenFor("anna@newssite.com", "anna");

        given()
                .auth().oauth2(token)
                .contentType("application/json")
                .body("{\"topicName\": \"Technology\", \"frequency\": \"daily\"}")
                .when()
                .post("/api/subscriptions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("topicName", equalTo("technology"))
                .body("frequency", equalTo("DAILY"))
                .body("createdAt", notNullValue());
    }

    @Test
    void create_withoutToken_returns401() {
        given()
                .contentType("application/json")
                .body("{\"topicName\": \"technology\", \"frequency\": \"DAILY\"}")
                .when()
                .post("/api/subscriptions")
                .then()
                .statusCode(401);
    }

    @Test
    void create_sameTopicTwice_returns409() {
        String token = tokenFor("anna@newssite.com", "anna");

        subscribe(token, "technology", "DAILY", 201);
        subscribe(token, "technology", "WEEKLY", 409);
    }

    @Test
    void create_unknownFrequency_returns400WithDanishMessage() {
        String token = tokenFor("anna@newssite.com", "anna");

        given()
                .auth().oauth2(token)
                .contentType("application/json")
                .body("{\"topicName\": \"technology\", \"frequency\": \"MONTHLY\"}")
                .when()
                .post("/api/subscriptions")
                .then()
                .statusCode(400)
                .body("message", equalTo("Frekvens skal være DAILY eller WEEKLY"));
    }

    @Test
    void create_unknownTopic_returns404() {
        String token = tokenFor("anna@newssite.com", "anna");

        subscribe(token, "findesikke", "DAILY", 404);
    }

    @Test
    void getMine_returnsOnlyOwnSubscriptions() {
        String annaToken = tokenFor("anna@newssite.com", "anna");
        String boToken = tokenFor("bo@newssite.com", "bo");
        subscribe(annaToken, "technology", "DAILY", 201);
        subscribe(annaToken, "science", "WEEKLY", 201);
        subscribe(boToken, "science", "DAILY", 201);

        given()
                .auth().oauth2(annaToken)
                .when()
                .get("/api/subscriptions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("topicName", contains("science", "technology"));
    }

    @Test
    void delete_removesSubscription() {
        String token = tokenFor("anna@newssite.com", "anna");
        subscribe(token, "technology", "DAILY", 201);

        given().auth().oauth2(token)
                .when().delete("/api/subscriptions/technology")
                .then().statusCode(200).body("topicName", equalTo("technology"));

        given().auth().oauth2(token)
                .when().get("/api/subscriptions")
                .then().statusCode(200).body("size()", equalTo(0));
    }

    @Test
    void delete_notSubscribed_returns404() {
        String token = tokenFor("anna@newssite.com", "anna");

        given().auth().oauth2(token)
                .when().delete("/api/subscriptions/technology")
                .then().statusCode(404);
    }

    @Test
    void delete_otherUsersSubscription_returns404AndKeepsIt() {
        String annaToken = tokenFor("anna@newssite.com", "anna");
        String boToken = tokenFor("bo@newssite.com", "bo");
        subscribe(boToken, "technology", "DAILY", 201);

        given().auth().oauth2(annaToken)
                .when().delete("/api/subscriptions/technology")
                .then().statusCode(404);

        given().auth().oauth2(boToken)
                .when().get("/api/subscriptions")
                .then().statusCode(200).body("size()", equalTo(1));
    }
}
