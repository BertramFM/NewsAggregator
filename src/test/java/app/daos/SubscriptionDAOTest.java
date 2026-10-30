package app.daos;

import app.config.HibernateTestConfig;
import app.entities.Frequency;
import app.entities.Subscription;
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

class SubscriptionDAOTest {

    private static EntityManagerFactory emf;
    private static SubscriptionDAO dao;
    private static UserDAO userDAO;
    private SeedData seed;
    private String email;

    @BeforeAll
    static void setUpAll() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        dao = new SubscriptionDAO(emf);
        userDAO = new UserDAO(emf);
    }

    @BeforeEach
    void setUp() {
        seed = DatabaseTestPopulator.populate(emf);
        email = seed.user().getEmail();
    }

    private User createOtherUser() {
        return userDAO.create(User.builder()
                .email("other@newssite.com")
                .passwordHash("hash")
                .userName("other")
                .build());
    }

    @Test
    void subscribe_createsSubscriptionWithCreatedAt() {
        Subscription created = dao.subscribe(email, "technology", Frequency.DAILY);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertEquals("technology", created.getTopic().getTopicName());
        assertEquals(Frequency.DAILY, created.getFrequency());
    }

    @Test
    void subscribe_topicNameIgnoresCaseAndSpaces() {
        Subscription created = dao.subscribe(email, " Technology ", Frequency.WEEKLY);

        assertEquals(seed.technology().getId(), created.getTopic().getId());
    }

    @Test
    void subscribe_sameTopicWithOtherFrequency_returns409() {
        dao.subscribe(email, "technology", Frequency.DAILY);

        ApiException ex = assertThrows(ApiException.class,
                () -> dao.subscribe(email, "technology", Frequency.WEEKLY));
        assertEquals(409, ex.getCode());
    }

    @Test
    void subscribe_unknownTopic_returns404() {
        ApiException ex = assertThrows(ApiException.class,
                () -> dao.subscribe(email, "findesikke", Frequency.DAILY));
        assertEquals(404, ex.getCode());
    }

    @Test
    void subscribe_unknownUser_returns404() {
        ApiException ex = assertThrows(ApiException.class,
                () -> dao.subscribe("nobody@newssite.com", "technology", Frequency.DAILY));
        assertEquals(404, ex.getCode());
    }

    @Test
    void subscribe_missingFrequency_returns400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> dao.subscribe(email, "technology", null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void findByUserEmail_returnsOnlyOwnSortedByTopic() {
        dao.subscribe(email, "technology", Frequency.DAILY);
        dao.subscribe(email, "science", Frequency.WEEKLY);
        User other = createOtherUser();
        dao.subscribe(other.getEmail(), "science", Frequency.DAILY);

        List<Subscription> result = dao.findByUserEmail(email);

        assertEquals(2, result.size());
        assertEquals("science", result.get(0).getTopic().getTopicName());
        assertEquals("technology", result.get(1).getTopic().getTopicName());
    }

    @Test
    void findByUserEmail_noSubscriptions_returnsEmptyList() {
        assertTrue(dao.findByUserEmail(email).isEmpty());
    }

    @Test
    void deleteForUser_removesSubscription() {
        dao.subscribe(email, "technology", Frequency.DAILY);

        dao.deleteForUser("Technology", email);

        assertTrue(dao.findByUserEmail(email).isEmpty());
    }

    @Test
    void deleteForUser_notSubscribed_returns404() {
        ApiException ex = assertThrows(ApiException.class,
                () -> dao.deleteForUser("technology", email));
        assertEquals(404, ex.getCode());
    }

    @Test
    void deleteForUser_cannotDeleteOtherUsersSubscription() {
        User other = createOtherUser();
        dao.subscribe(other.getEmail(), "technology", Frequency.DAILY);

        ApiException ex = assertThrows(ApiException.class,
                () -> dao.deleteForUser("technology", email));

        assertEquals(404, ex.getCode());
        assertEquals(1, dao.findByUserEmail(other.getEmail()).size());
    }
}
