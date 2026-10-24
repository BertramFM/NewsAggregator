package app.daos;

import app.config.HibernateTestConfig;
import app.entities.Frequency;
import app.entities.Subscription;
import app.entities.Topic;
import app.entities.User;
import app.exceptions.ApiException;
import app.testUtils.DatabaseTestPopulator;
import app.testUtils.DatabaseTestPopulator.SeedData;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TopicDAOTest {

    private static EntityManagerFactory emf;
    private static TopicDAO dao;
    private static SubscriptionDAO subscriptionDAO;
    private static UserDAO userDAO;
    private SeedData seed;

    @BeforeAll
    static void setUpAll() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        dao = new TopicDAO(emf);
        subscriptionDAO = new SubscriptionDAO(emf);
        userDAO = new UserDAO(emf);
    }

    @BeforeEach
    void setUp() {
        seed = DatabaseTestPopulator.populate(emf);
    }

    private void subscribe(User user, Topic topic, Frequency frequency) {
        subscriptionDAO.create(Subscription.builder()
                .user(user)
                .topic(topic)
                .frequency(frequency)
                .build());
    }

    @Test
    void findTopicsWithSubscribedUsers_returnEachTopicOnce() {
        User secondUser = userDAO.create(User.builder()
                .email("falken@newssite.com")
                .passwordHash("hash")
                .userName("falken")
                .build());

        // To brugere på science DAILY: emnet må kun komme med én gang
        subscribe(seed.user(), seed.science(), Frequency.DAILY);
        subscribe(secondUser, seed.science(), Frequency.DAILY);

        List<Topic> daily = dao.findTopicsWithSubscribedUsers(Frequency.DAILY);

        assertEquals(List.of("science"), daily.stream().map(Topic::getTopicName).toList());
    }

    @Test
    void findTopicsWithSubscribedUsers_filtersOnFrequency() {
        User secondUser = userDAO.create(User.builder()
                .email("falken@newssite.com")
                .passwordHash("hash")
                .userName("falken")
                .build());

        subscribe(seed.user(), seed.science(), Frequency.DAILY);

        subscribe(seed.user(), seed.technology(), Frequency.WEEKLY);
        subscribe(secondUser, seed.science(), Frequency.WEEKLY);


        assertEquals(List.of("science"),
                dao.findTopicsWithSubscribedUsers(Frequency.DAILY).stream().map(Topic::getTopicName).toList());

        assertEquals(List.of("technology", "science"),
                dao.findTopicsWithSubscribedUsers(Frequency.WEEKLY).stream().map(Topic::getTopicName).toList());
    }

    @Test
    void findTopicsWithSubscribedUsers_emptyWhenNoSubscriptions() {
        // Jeg har ingen abonnementer i min populator
        assertTrue(dao.findTopicsWithSubscribedUsers(Frequency.DAILY).isEmpty());
        assertTrue(dao.findTopicsWithSubscribedUsers(Frequency.WEEKLY).isEmpty());
    }

    @Test
    void findTopicsWithSubscribedUsers_throwsWhenFrequencyIsNull() {
        assertThrows(ApiException.class, () -> dao.findTopicsWithSubscribedUsers(null));
    }
}
