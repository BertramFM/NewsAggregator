package app;

import app.config.HibernateConfig;
import app.daos.ArticleDAO;
import app.daos.SourceDAO;
import app.daos.TopicDAO;
import app.server.JavalinServer;
import app.services.*;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("app");

    public static void main(String[] args) {
        // System.out.println(new GeminiClient().generate("What is the capital of France?"));

        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        SyncService syncService = new SyncService(new ArticleDAO(emf), new TopicDAO(emf), new SourceDAO(emf),
                new GuardianUrlBuilder(), new APIReader());
        SyncSchedule syncSchedule = new SyncSchedule(syncService);

        // Syncs articles from the last day every 30 minutes
        syncSchedule.start();

        JavalinServer.startServer(emf, 7070);
    }
}
