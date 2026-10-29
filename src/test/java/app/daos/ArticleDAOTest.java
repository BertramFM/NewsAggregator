package app.daos;

import app.config.HibernateTestConfig;
import app.entities.Article;
import app.entities.DigestLog;
import app.entities.Topic;
import app.exceptions.ApiException;
import app.testUtils.DatabaseTestPopulator;
import app.testUtils.DatabaseTestPopulator.SeedData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArticleDAOTest {

    private static EntityManagerFactory emf;
    private static ArticleDAO dao;
    private SeedData seed;

    @BeforeAll
    static void setUpAll() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        dao = new ArticleDAO(emf);
    }

    @BeforeEach
    void setUp() {
        seed = DatabaseTestPopulator.populate(emf);
    }

    @Test
    void getById_withSourceAndTopicLoaded() {
        Article found = dao.getById(seed.article1().getId());

        assertNotNull(found);

        assertEquals("Scientists find new planet", found.getTitle());
        // EntityManager er lukket her
        assertEquals("The Guardian", found.getSource().getSourceName());
        assertEquals("science", found.getTopic().getTopicName());
    }

    @Test
    void getById_nullWhenArticleDoesNotExist() {
        assertNull(dao.getById(99999999999L));
    }

    @Test
    void getAll_sortedByPublishedAtAndFetchedWithSourceAndTopic() {
        List<Article> articles = dao.getAll();

        assertEquals(2, articles.size());
        assertEquals("New chip announced", articles.get(0).getTitle());
        assertEquals("Scientists find new planet", articles.get(1).getTitle());
        articles.forEach(a -> {
            assertNotNull(a.getSource().getSourceName());
            assertNotNull(a.getTopic().getTopicName());
        });
    }

    @Test
    void existsByExternalId_trueForExistingFalseOtherwise() {
        assertTrue(dao.existsByExternalId("test/article1"));
        assertFalse(dao.existsByExternalId("findes/ikke"));
    }

    @Test
    void delete_removesArticle() {
        Long id = seed.article1().getId();

        assertEquals(2, dao.getAll().size());
        assertTrue(dao.delete(id));

        assertNull(dao.getById(id));
        assertEquals(1, dao.getAll().size());
    }

    @Test
    void delete_throwsWhenArticleDoesNotExist() {
        assertThrows(ApiException.class, () -> dao.delete(999_999L));
    }

    @Test
    void delete_articleInDigestLog_removesLinkAndKeepsDigestLog() {
        Long articleId = seed.article1().getId();
        Long logId = seed.digestLog().getId();

        assertTrue(dao.delete(articleId));

        assertNull(dao.getById(articleId));
        try (EntityManager em = emf.createEntityManager()) {
            DigestLog log = em.find(DigestLog.class, logId);
            assertNotNull(log);
            assertEquals(1, log.getArticles().size());
            assertEquals("New chip announced", log.getArticles().iterator().next().getTitle());
        }
    }

    @Test
    void findLatestPublishedAt_returnNewestForThatTopic(){
        Article newerScience = Article.builder()
                .externalId("test/article3")
                .title("Newest science update")
                .publishedAt(LocalDateTime.of(2026, 10, 20, 8, 30))
                .source(seed.source())
                .topic(seed.science())
                .build();
        dao.create(newerScience);

        assertEquals(LocalDateTime.of(2026, 10, 20, 8, 30),
                dao.findLatestPublishedAt(seed.science().getId()));

        assertEquals(LocalDateTime.of(2026, 10, 19, 12, 0),
                dao.findLatestPublishedAt(seed.technology().getId()));
    }

    @Test
    void findLatestPublishedAt_returnNullWhenTopicHasNoArticles(){
        Topic empty = new TopicDAO(emf).create(Topic.builder().topicName("finance").build());
        assertNull(dao.findLatestPublishedAt(empty.getId()));
    }

    // ======== findByTopicAndPeriod ========

    private Article scienceArticle(String externalId, LocalDateTime publishedAt) {
        return dao.create(Article.builder()
                .externalId(externalId)
                .title("Science " + externalId)
                .publishedAt(publishedAt)
                .source(seed.source())
                .topic(seed.science())
                .build());
    }

    @Test
    void findByTopicAndPeriod_returnsOnlyThatTopicInPeriod_newestFirst() {
        scienceArticle("test/science-20", LocalDateTime.of(2026, 10, 20, 8, 30));
        scienceArticle("test/science-17", LocalDateTime.of(2026, 10, 17, 23, 59)); // før perioden

        List<Article> result = dao.findByTopicAndPeriod(seed.science().getId(),
                LocalDateTime.of(2026, 10, 18, 0, 0), LocalDateTime.of(2026, 10, 21, 0, 0), 25);

        // article2 (technology, 19. okt) ligger i perioden, men er et andet emne
        assertEquals(List.of("test/science-20", "test/article1"),
                result.stream().map(Article::getExternalId).toList());
    }

    @Test
    void findByTopicAndPeriod_respectsLimit() {
        scienceArticle("test/science-20", LocalDateTime.of(2026, 10, 20, 8, 30));

        List<Article> result = dao.findByTopicAndPeriod(seed.science().getId(),
                LocalDateTime.of(2026, 10, 18, 0, 0), LocalDateTime.of(2026, 10, 21, 0, 0), 1);

        assertEquals(1, result.size());
        assertEquals("test/science-20", result.get(0).getExternalId());
    }

    @Test
    void findByTopicAndPeriod_startInclusiveEndExclusive() {
        scienceArticle("test/midnight", LocalDateTime.of(2026, 10, 19, 0, 0));

        List<Article> day18 = dao.findByTopicAndPeriod(seed.science().getId(),
                LocalDateTime.of(2026, 10, 18, 0, 0), LocalDateTime.of(2026, 10, 19, 0, 0), 25);
        List<Article> day19 = dao.findByTopicAndPeriod(seed.science().getId(),
                LocalDateTime.of(2026, 10, 19, 0, 0), LocalDateTime.of(2026, 10, 20, 0, 0), 25);

        // Artiklen præcis ved midnat hører kun til den nye dag
        assertEquals(List.of("test/article1"), day18.stream().map(Article::getExternalId).toList());
        assertEquals(List.of("test/midnight"), day19.stream().map(Article::getExternalId).toList());
    }

    @Test
    void findByTopicAndPeriod_emptyWhenNoArticlesInPeriod() {
        assertTrue(dao.findByTopicAndPeriod(seed.technology().getId(),
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0), 25).isEmpty());
    }

    @Test
    void findByTopicAndPeriod_throwsOnInvalidInput() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 18, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 19, 0, 0);
        Long topicId = seed.science().getId();

        assertThrows(ApiException.class, () -> dao.findByTopicAndPeriod(null, start, end, 25));
        assertThrows(ApiException.class, () -> dao.findByTopicAndPeriod(topicId, null, end, 25));
        assertThrows(ApiException.class, () -> dao.findByTopicAndPeriod(topicId, start, null, 25));
        assertThrows(ApiException.class, () -> dao.findByTopicAndPeriod(topicId, start, end, 0));
    }

    @Test
    void findArticleWithoutSummaryAndSentiment_onlyArticlesWithoutSummaryNewestFirst() {
        dao.updateArticleSummaryAndSentiment(seed.article2().getId(), "Already summarized", 0.5);
        scienceArticle("test/science-20", LocalDateTime.of(2026, 10, 20, 8, 30));

        List<Article> result = dao.findArticleWithoutSummaryAndSentiment(10);

        assertEquals(List.of("test/science-20", "test/article1"),
                result.stream().map(Article::getExternalId).toList());
    }

    @Test
    void findArticleWithoutSummaryAndSentiment_respectsLimit() {
        assertEquals(1, dao.findArticleWithoutSummaryAndSentiment(1).size());
    }

    @Test
    void findArticleWithoutSummaryAndSentiment_throwsOnInvalidLimit() {
        assertThrows(ApiException.class, () -> dao.findArticleWithoutSummaryAndSentiment(0));
    }

    @Test
    void updateArticleSummaryAndSentiment_setsSummaryAndScore() {
        Long id = seed.article1().getId();

        dao.updateArticleSummaryAndSentiment(id, "A new planet was found.", -0.25);

        Article found = dao.getById(id);
        assertEquals("A new planet was found.", found.getAiSummary());
        assertEquals(-0.25, found.getSentimentScore());

        assertEquals("Scientists find new planet", found.getTitle());
    }

    @Test
    void updateArticleSummaryAndSentiment_throws404WhenArticleDoesNotExist() {
        ApiException e = assertThrows(ApiException.class,
                () -> dao.updateArticleSummaryAndSentiment(99999999999L, "x", 0.0));
        assertEquals(404, e.getCode());
    }

    @Test
    void updateArticleSummaryAndSentiment_throwsOnNullId() {
        assertThrows(ApiException.class, () -> dao.updateArticleSummaryAndSentiment(null, "x", 0.0));
    }
}
