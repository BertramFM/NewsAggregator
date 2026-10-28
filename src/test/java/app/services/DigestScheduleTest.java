package app.services;

import app.config.HibernateTestConfig;
import app.daos.ArticleDAO;
import app.daos.DigestLogDAO;
import app.daos.SubscriptionDAO;
import app.daos.TopicDAO;
import app.entities.Article;
import app.entities.Frequency;
import app.entities.Subscription;
import app.entities.Topic;
import app.exceptions.ApiException;
import app.services.DigestService.Period;
import app.testUtils.DatabaseTestPopulator;
import app.testUtils.DatabaseTestPopulator.SeedData;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DigestScheduleTest {

    private static EntityManagerFactory emf;
    private static ArticleDAO articleDAO;
    private static DigestLogDAO digestLogDAO;
    private static SubscriptionDAO subscriptionDAO;
    private static TopicDAO topicDAO;
    private SeedData seed;

    private final List<String> prompts = new ArrayList<>();
    private String failForTopic; // hvis sat: den fake AI kaster 429 for prompts om dette emne
    private DigestSchedule schedule;

    @BeforeAll
    static void setUpAll() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        articleDAO = new ArticleDAO(emf);
        digestLogDAO = new DigestLogDAO(emf);
        subscriptionDAO = new SubscriptionDAO(emf);
        topicDAO = new TopicDAO(emf);
    }

    @BeforeEach
    void setUp() {
        seed = DatabaseTestPopulator.populate(emf);
        prompts.clear();
        failForTopic = null;

        AiClient fakeAi = prompt -> {
            if (failForTopic != null && prompt.contains("topic " + failForTopic)) {
                throw new ApiException(429, "Fake rate limit");
            }
            prompts.add(prompt);
            return "AI digest";
        };
        schedule = new DigestSchedule(new DigestService(articleDAO, digestLogDAO, fakeAi), topicDAO);
    }

    private void subscribe(Topic topic, Frequency frequency) {
        subscriptionDAO.create(Subscription.builder()
                .user(seed.user())
                .topic(topic)
                .frequency(frequency)
                .build());
    }

    private void article(String externalId, Topic topic, LocalDateTime publishedAt) {
        articleDAO.create(Article.builder()
                .externalId(externalId)
                .title("Title " + externalId)
                .publishedAt(publishedAt)
                .source(seed.source())
                .topic(topic)
                .build());
    }

    private LocalDateTime insideLastPeriod(Frequency frequency) {
        Period p = DigestService.lastCompletedPeriod(frequency, LocalDateTime.now(ZoneOffset.UTC));
        return p.startDate().plusHours(12);
    }

    private boolean hasDigest(Topic topic, Frequency frequency) {
        Period p = DigestService.lastCompletedPeriod(frequency, LocalDateTime.now(ZoneOffset.UTC));
        return digestLogDAO.existByTopicAndFrequencyPeriod(topic.getId(), frequency, p.startDate());
    }


    @Test
    void run_withoutSubscriptions_createsNothingAndNeverCallsAi() {
        article("test/tech-yesterday", seed.technology(), insideLastPeriod(Frequency.DAILY));

        schedule.run();

        assertTrue(prompts.isEmpty());
        assertEquals(1, digestLogDAO.getAll().size());
    }

    @Test
    void run_withSubscriptionButNoArticlesInPeriod_createsNothing() {
        subscribe(seed.technology(), Frequency.DAILY);

        schedule.run();

        assertTrue(prompts.isEmpty());
        assertEquals(1, digestLogDAO.getAll().size());
    }

    @Test
    void run_createsDailyDigestForSubscribedTopic() {
        subscribe(seed.technology(), Frequency.DAILY);
        article("test/tech-yesterday", seed.technology(), insideLastPeriod(Frequency.DAILY));

        schedule.run();

        assertEquals(1, prompts.size());
        assertTrue(prompts.get(0).contains("Title test/tech-yesterday"));
        assertTrue(hasDigest(seed.technology(), Frequency.DAILY));
        assertEquals(2, digestLogDAO.getAll().size());
    }

    @Test
    void run_twice_doesNotCreateDuplicateOrCallAiAgain() {
        subscribe(seed.technology(), Frequency.DAILY);
        article("test/tech-yesterday", seed.technology(), insideLastPeriod(Frequency.DAILY));

        schedule.run();
        schedule.run();

        assertEquals(1, prompts.size());
        assertEquals(2, digestLogDAO.getAll().size());
    }

    @Test
    void run_createsWeeklyDigestForWeeklySubscription() {
        subscribe(seed.technology(), Frequency.WEEKLY);
        article("test/tech-last-week", seed.technology(), insideLastPeriod(Frequency.WEEKLY));

        schedule.run();

        assertTrue(hasDigest(seed.technology(), Frequency.WEEKLY));
        assertFalse(hasDigest(seed.technology(), Frequency.DAILY));
    }

    @Test
    void run_aiFailureOnOneTopic_otherTopicsStillGetDigest() {
        subscribe(seed.science(), Frequency.DAILY);
        subscribe(seed.technology(), Frequency.DAILY);
        article("test/sci-yesterday", seed.science(), insideLastPeriod(Frequency.DAILY));
        article("test/tech-yesterday", seed.technology(), insideLastPeriod(Frequency.DAILY));
        failForTopic = "science";

        assertDoesNotThrow(() -> schedule.run());

        assertFalse(hasDigest(seed.science(), Frequency.DAILY));
        assertTrue(hasDigest(seed.technology(), Frequency.DAILY));
    }

    @Test
    void run_retriesFailedTopicOnNextRun() {
        subscribe(seed.science(), Frequency.DAILY);
        article("test/sci-yesterday", seed.science(), insideLastPeriod(Frequency.DAILY));

        failForTopic = "science";
        schedule.run();
        assertFalse(hasDigest(seed.science(), Frequency.DAILY));

        failForTopic = null;
        schedule.run();
        assertTrue(hasDigest(seed.science(), Frequency.DAILY));
    }
}
