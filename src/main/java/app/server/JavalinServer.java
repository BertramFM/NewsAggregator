package app.server;

import app.config.TokenSettings;
import app.controllers.ArticleController;
import app.controllers.DigestController;
import app.controllers.SecurityController;
import app.controllers.SubscriptionController;
import app.daos.ArticleDAO;
import app.daos.DigestLogDAO;
import app.daos.SecurityDAO;
import app.daos.SubscriptionDAO;
import app.exceptions.ApiException;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import jakarta.persistence.EntityManagerFactory;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@NoArgsConstructor
public class JavalinServer {
    private static final Logger log = LoggerFactory.getLogger(JavalinServer.class);

    public static Javalin startServer(EntityManagerFactory emf, int serverPort, TokenSettings tokenSettings) {
        ArticleController articleController = new ArticleController(new ArticleDAO(emf));
        SecurityController securityController = new SecurityController(new SecurityDAO(emf), tokenSettings);
        SubscriptionController subscriptionController = new SubscriptionController(new SubscriptionDAO(emf));
        DigestController digestController = new DigestController(new DigestLogDAO(emf));

        return Javalin.start(config -> {
            config.jetty.port = serverPort;

            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));

            config.routes.beforeMatched(securityController::authenticate);
            config.routes.beforeMatched(securityController::authorize);

            config.routes.post("api/auth/register", securityController::register, AppRole.ANYONE, AppRole.ADMIN, AppRole.USER);
            config.routes.post("api/auth/login", securityController::login, AppRole.ANYONE, AppRole.ADMIN, AppRole.USER);

            config.routes.get("api/articles", articleController::getArticles, AppRole.ANYONE, AppRole.ADMIN, AppRole.USER);
            config.routes.get("api/articles/{id}", articleController::getById, AppRole.ANYONE, AppRole.USER, AppRole.ADMIN);

            config.routes.delete("api/articles/{id}", articleController::delete, AppRole.ADMIN);

            config.routes.post("api/subscriptions", subscriptionController::create, AppRole.USER, AppRole.ADMIN);
            config.routes.get("api/subscriptions", subscriptionController::getMine, AppRole.USER, AppRole.ADMIN);
            config.routes.delete("api/subscriptions/{topicName}", subscriptionController::delete, AppRole.USER);

            config.routes.get("api/digests", digestController::getMine, AppRole.USER, AppRole.ADMIN);

            config.routes.get("api/health", ctx -> ctx.result("OK"), AppRole.ANYONE);

            config.routes.exception(ApiException.class, (e, ctx) -> {
                ctx.status(e.getCode());
                ctx.json(Map.of("status", e.getCode(), "message", e.getMessage()));
            });

            config.routes.exception(Exception.class, (e, ctx) -> {
                log.error("Unhandled exception: ", e);
                ctx.status(500);
                ctx.json(Map.of("status", 500, "message", "Internal server error"));
            });
        });
    }
}
