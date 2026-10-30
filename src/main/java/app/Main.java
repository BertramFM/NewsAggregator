package app;

import app.config.HibernateConfig;
import app.config.TokenSettings;
import app.daos.ArticleDAO;
import app.daos.DigestLogDAO;
import app.daos.SourceDAO;
import app.daos.TopicDAO;
import app.server.JavalinServer;
import app.services.*;
import jakarta.persistence.EntityManagerFactory;

public class Main {
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

        // Syncs articles every 45 minutes from The Guardian
        syncSchedule.start();
        // Creates summaries of the previous day's articles based on Topic. Only if there are users subscribed to the topic.
        digestSchedule.start();
        // Creates summaries and sentiment scores for articles that don't have them yet. every 15 minutes.
        summarySchedule.start();


        JavalinServer.startServer(emf, 7070, TokenSettings.fromEnvironment());
    }
}
