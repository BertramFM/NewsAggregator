package app.daos;

import app.config.HibernateTestConfig;
import app.entities.DigestLog;
import app.entities.Frequency;
import app.entities.Subscription;
import app.entities.Topic;
import app.exceptions.ApiException;
import app.testUtils.DatabaseTestPopulator;
import app.testUtils.DatabaseTestPopulator.SeedData;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DigestLogDAOTest {

    private static EntityManagerFactory emf;
    private static DigestLogDAO dao;
    private static SubscriptionDAO subscriptionDAO;
    private SeedData seed;

    // Samme periodStart som populatorens DigestLog
    private static final LocalDateTime SEEDED_START = LocalDateTime.of(2026, 10, 13, 0, 0);

    @BeforeAll
    static void setUpAll() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        dao = new DigestLogDAO(emf);
        subscriptionDAO = new SubscriptionDAO(emf);
    }

    @BeforeEach
    void setUp() {
        seed = DatabaseTestPopulator.populate(emf);
    }

    @Test
    void existByTopicAndFrequencyPeriod_trueForSeededDigest() {
        assertTrue(dao.existByTopicAndFrequencyPeriod(seed.science().getId(), Frequency.DAILY, SEEDED_START));
    }

    @Test
    void existByTopicAndFrequencyPeriod_falseWhenAnyKeyPartDiffers() {
        Long science = seed.science().getId();

        assertFalse(dao.existByTopicAndFrequencyPeriod(science, Frequency.WEEKLY, SEEDED_START));
        assertFalse(dao.existByTopicAndFrequencyPeriod(seed.technology().getId(), Frequency.DAILY, SEEDED_START));
        assertFalse(dao.existByTopicAndFrequencyPeriod(science, Frequency.DAILY, SEEDED_START.plusDays(1)));
        // Tidspunktet tæller med: samme dag, men ikke præcis midnat
        assertFalse(dao.existByTopicAndFrequencyPeriod(science, Frequency.DAILY, SEEDED_START.plusHours(1)));
    }

    @Test
    void existByTopicAndFrequencyPeriod_throwsOnNullInput() {
        Long science = seed.science().getId();

        assertThrows(ApiException.class, () -> dao.existByTopicAndFrequencyPeriod(null, Frequency.DAILY, SEEDED_START));
        assertThrows(ApiException.class, () -> dao.existByTopicAndFrequencyPeriod(science, null, SEEDED_START));
        assertThrows(ApiException.class, () -> dao.existByTopicAndFrequencyPeriod(science, Frequency.DAILY, null));
    }

    private void subscribeAt(Topic topic, Frequency frequency, LocalDateTime createdAt) {
        subscriptionDAO.create(Subscription.builder()
                .user(seed.user())
                .topic(topic)
                .frequency(frequency)
                .createdAt(createdAt)
                .build());
    }

    @Test
    void findForUser_subscribedBeforeDigestWasGenerated_returnsDigest() {
        subscribeAt(seed.science(), Frequency.DAILY, LocalDateTime.of(2026, 1, 1, 0, 0));

        List<DigestLog> result = dao.findForUser(seed.user().getEmail());

        assertEquals(1, result.size());
        assertEquals(seed.digestLog().getId(), result.get(0).getId());
        assertEquals("science", result.get(0).getTopic().getTopicName());
    }

    @Test
    void findForUser_subscribedAfterDigestWasGenerated_returnsEmpty() {
        subscribeAt(seed.science(), Frequency.DAILY, LocalDateTime.now().plusMinutes(1));

        assertTrue(dao.findForUser(seed.user().getEmail()).isEmpty());
    }

    @Test
    void findForUser_otherFrequency_returnsEmpty() {
        subscribeAt(seed.science(), Frequency.WEEKLY, LocalDateTime.of(2026, 1, 1, 0, 0));

        assertTrue(dao.findForUser(seed.user().getEmail()).isEmpty());
    }

    @Test
    void findForUser_otherTopic_returnsEmpty() {
        subscribeAt(seed.technology(), Frequency.DAILY, LocalDateTime.of(2026, 1, 1, 0, 0));

        assertTrue(dao.findForUser(seed.user().getEmail()).isEmpty());
    }

    @Test
    void findForUser_noSubscriptions_returnsEmpty() {
        assertTrue(dao.findForUser(seed.user().getEmail()).isEmpty());
    }

    @Test
    void findForUser_missingEmail_returns400() {
        ApiException ex = assertThrows(ApiException.class, () -> dao.findForUser(null));
        assertEquals(400, ex.getCode());
    }
}
