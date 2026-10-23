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
}