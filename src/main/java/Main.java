import app.configs.HibernateConfig;
import app.services.GuardianTopic;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("app");

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        Javalin app = Javalin.create().start(7070);

    }
}
