package app.daos;

import app.config.HibernateTestConfig;
import app.entities.Frequency;
import app.exceptions.ApiException;
import app.testUtils.DatabaseTestPopulator;
import app.testUtils.DatabaseTestPopulator.SeedData;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DigestLogDAOTest {

    private static EntityManagerFactory emf;
    private static DigestLogDAO dao;
    private SeedData seed;

    // Samme periodStart som populatorens DigestLog
    private static final LocalDateTime SEEDED_START = LocalDateTime.of(2026, 10, 13, 0, 0);

    @BeforeAll
    static void setUpAll() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        dao = new DigestLogDAO(emf);
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
}
