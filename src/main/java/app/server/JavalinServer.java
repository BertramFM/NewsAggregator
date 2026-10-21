package app.server;

import app.controllers.ArticleController;
import app.daos.ArticleDAO;
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

    public static Javalin startServer(EntityManagerFactory emf, int serverPort) {
        ArticleController articleController = new ArticleController(new ArticleDAO(emf));

        return Javalin.start(config -> {
            config.jetty.port = serverPort;

            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));

            config.routes.get("/articles", articleController::getArticles);
            config.routes.get("/articles/{id}", articleController::getById);
            config.routes.delete("/articles/{id}", articleController::delete);

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
