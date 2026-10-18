import app.configs.HibernateConfig;
import app.daos.ArticleDAO;
import app.daos.SourceDAO;
import app.daos.TopicDAO;
import app.services.APIReader;
import app.services.GuardianTopic;
import app.services.GuardianUrlBuilder;
import app.services.SyncService;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("app");

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        SyncService syncService = new SyncService(new ArticleDAO(emf), new TopicDAO(emf), new SourceDAO(emf),
                new GuardianUrlBuilder(), new APIReader());
        syncService.syncFromDate(LocalDate.now().minusDays(1));
        Javalin app = Javalin.create().start(7070);
    }
}
