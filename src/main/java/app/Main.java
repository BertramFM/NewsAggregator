package app;

import app.config.HibernateConfig;
import app.daos.ArticleDAO;
import app.daos.DigestLogDAO;
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
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        SyncService syncService = new SyncService(new ArticleDAO(emf), new TopicDAO(emf), new SourceDAO(emf),
                new GuardianUrlBuilder(), new APIReader());
        SyncSchedule syncSchedule = new SyncSchedule(syncService);
        AiClient aiClient = new GeminiClient();
        DigestService digestService = new DigestService(new ArticleDAO(emf), new DigestLogDAO(emf), aiClient);
        DigestSchedule digestSchedule = new DigestSchedule(digestService, new TopicDAO(emf));
        ArticleSummarySentimentService summaryService = new ArticleSummarySentimentService(new ArticleDAO(emf), aiClient);
        ArticleSummarySentimentSchedule summarySchedule = new ArticleSummarySentimentSchedule(summaryService);

        // Syncs articles from the last day every 45 minutes
        syncSchedule.start();
        digestSchedule.start();
        summarySchedule.start();


        JavalinServer.startServer(emf, 7070);
    }
}
