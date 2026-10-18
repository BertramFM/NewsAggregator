import app.configs.HibernateConfig;
import app.controllers.ArticleController;
import app.daos.ArticleDAO;
import app.daos.SourceDAO;
import app.daos.TopicDAO;
import app.exceptions.ApiException;
import app.services.APIReader;
import app.services.GuardianUrlBuilder;
import app.services.SyncService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("app");

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        ArticleController articleController = new ArticleController(new ArticleDAO(emf));

        SyncService syncService = new SyncService(new ArticleDAO(emf), new TopicDAO(emf), new SourceDAO(emf),
                new GuardianUrlBuilder(), new APIReader());
        syncService.syncFromDate(LocalDate.now().minusDays(1));


        Javalin app = Javalin.start(config -> {
            config.jetty.port = 7070;

            // Disable date array
            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));

            config.routes.get("/articles", articleController::getArticles);
            config.routes.get("/article/{id}", articleController::getById);
            config.routes.delete("/article/{id}", articleController::delete);

            config.routes.exception(ApiException.class, (e, ctx) -> {
                ctx.status(e.getCode());
                ctx.json(Map.of("status", e.getCode(), "message", e.getMessage()));
            });

            config.routes.exception(ApiException.class, (e, ctx) -> {
                ctx.status(e.getCode());
                ctx.json(Map.of("status", e.getCode(), "message", e.getMessage()));
            });
        });
    }
}
