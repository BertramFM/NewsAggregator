package app.services;

import app.config.HibernateTestConfig;
import app.daos.ArticleDAO;
import app.daos.DigestLogDAO;
import app.entities.DigestLog;
import app.entities.Frequency;
import app.services.DigestService.Period;
import app.testUtils.DatabaseTestPopulator;
import app.testUtils.DatabaseTestPopulator.SeedData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DigestServiceTest {

    private static EntityManagerFactory emf;
    private static DigestLogDAO digestLogDAO;
    private static ArticleDAO articleDAO;
    private SeedData seed;

    private final List<String> prompts = new ArrayList<>();
    private DigestService digestService;

    @BeforeAll
    static void setUpAll() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        digestLogDAO = new DigestLogDAO(emf);
        articleDAO = new ArticleDAO(emf);
    }

    @BeforeEach
    void setUp() {
        seed = DatabaseTestPopulator.populate(emf);
        prompts.clear();
        digestService = new DigestService(articleDAO, digestLogDAO, prompt -> {
            prompts.add(prompt);
            return "AI digest";
        });
    }


    @Test
    void lastCompletedPeriod_daily_isYesterdayMidnightToTodayMidnight() {
        Period p = DigestService.lastCompletedPeriod(Frequency.DAILY, LocalDateTime.of(2026, 10, 21, 10, 30));

        assertEquals(LocalDateTime.of(2026, 10, 20, 0, 0), p.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 21, 0, 0), p.endDate());
    }

    @Test
    void lastCompletedPeriod_weekly_midWeek_isPreviousMondayToThisMonday() {
        // onsdag
        Period p = DigestService.lastCompletedPeriod(Frequency.WEEKLY, LocalDateTime.of(2026, 10, 21, 10, 30));

        assertEquals(LocalDateTime.of(2026, 10, 12, 0, 0), p.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 19, 0, 0), p.endDate());
    }

    @Test
    void lastCompletedPeriod_weekly_onMonday_endsToday() {
        // mandag
        Period p = DigestService.lastCompletedPeriod(Frequency.WEEKLY, LocalDateTime.of(2026, 10, 26, 0, 0));

        assertEquals(LocalDateTime.of(2026, 10, 19, 0, 0), p.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 26, 0, 0), p.endDate());
    }

    @Test
    void lastCompletedPeriod_weekly_onSunday_stillInCurrentWeek() {
        // Søndag
        Period p = DigestService.lastCompletedPeriod(Frequency.WEEKLY, LocalDateTime.of(2026, 10, 25, 23, 59));

        assertEquals(LocalDateTime.of(2026, 10, 12, 0, 0), p.startDate());
        assertEquals(LocalDateTime.of(2026, 10, 19, 0, 0), p.endDate());
    }

    @Test
    void lastCompletedPeriod_throwsOnNullFrequency() {
        assertThrows(IllegalArgumentException.class,
                () -> DigestService.lastCompletedPeriod(null, LocalDateTime.of(2026, 10, 21, 10, 30)));
    }


    @Test
    void createDigest_createsAndPersistsDigestWithArticlesInPeriod() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 19, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 20, 0, 0);

        DigestLog created = digestService.createDigest(seed.technology(), Frequency.DAILY, start, end);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("AI digest", created.getDigestText());
        assertEquals(Frequency.DAILY, created.getFrequency());
        assertEquals(start, created.getPeriodStart());
        assertEquals(end, created.getPeriodEnd());

        assertTrue(digestLogDAO.existByTopicAndFrequencyPeriod(seed.technology().getId(), Frequency.DAILY, start));

        try (EntityManager em = emf.createEntityManager()) {
            DigestLog fromDb = em.find(DigestLog.class, created.getId());

            List<Long> articleIds = fromDb.getArticles().stream()
                    .map(a -> a.getId())
                    .toList();

            assertEquals(List.of(seed.article2().getId()), articleIds);
        }
    }

    @Test
    void createDigest_sendsTopicAndTitlesToAi() {
        digestService.createDigest(seed.science(), Frequency.WEEKLY,
                LocalDateTime.of(2026, 10, 12, 0, 0), LocalDateTime.of(2026, 10, 19, 0, 0).plusDays(1));

        assertEquals(1, prompts.size());
        String prompt = prompts.get(0);
        assertTrue(prompt.contains("science"));
        assertTrue(prompt.contains(seed.article1().getTitle()));
        assertTrue(prompt.contains(seed.article1().getFullText()));
        assertFalse(prompt.contains(seed.article2().getTitle()), "Artikel fra andet topic må ikke med");
    }

    @Test
    void createDigest_returnsNullAndSkipsAiWhenDigestAlreadyExists() {
        DigestLog result = digestService.createDigest(seed.science(), Frequency.DAILY,
                LocalDateTime.of(2026, 10, 13, 0, 0), LocalDateTime.of(2026, 10, 20, 0, 0));

        assertNull(result);
        assertTrue(prompts.isEmpty());
        assertEquals(1, digestLogDAO.getAll().size());
    }

    @Test
    void createDigest_returnsNullAndSkipsAiWhenNoArticlesInPeriod() {
        DigestLog result = digestService.createDigest(seed.technology(), Frequency.DAILY,
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0));

        assertNull(result);
        assertTrue(prompts.isEmpty());
        assertEquals(1, digestLogDAO.getAll().size());
    }

    @Test
    void createDigest_endIsExclusive() {
        DigestLog result = digestService.createDigest(seed.technology(), Frequency.DAILY,
                LocalDateTime.of(2026, 10, 19, 0, 0), LocalDateTime.of(2026, 10, 19, 12, 0));

        assertNull(result);
    }

    @Test
    void createDigest_secondCallForSamePeriodReturnsNull() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 19, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 20, 0, 0);

        assertNotNull(digestService.createDigest(seed.technology(), Frequency.DAILY, start, end));
        assertNull(digestService.createDigest(seed.technology(), Frequency.DAILY, start, end));

        assertEquals(1, prompts.size());
    }
}
