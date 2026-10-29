package app.services;

import app.config.HibernateTestConfig;
import app.daos.ArticleDAO;
import app.entities.Article;
import app.exceptions.ApiException;
import app.testUtils.DatabaseTestPopulator;
import app.testUtils.DatabaseTestPopulator.SeedData;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ArticleSummarySentimentServiceTest {

    private static EntityManagerFactory emf;
    private static ArticleDAO articleDAO;
    private SeedData seed;

    private final List<String> prompts = new ArrayList<>();

    @BeforeAll
    static void setUpAll() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        articleDAO = new ArticleDAO(emf);
    }

    @BeforeEach
    void setUp() {
        seed = DatabaseTestPopulator.populate(emf);
        prompts.clear();
    }

    private ArticleSummarySentimentService serviceAnswering(String aiResponse) {
        return new ArticleSummarySentimentService(articleDAO, prompt -> {
            prompts.add(prompt);
            return aiResponse;
        });
    }

    private static String result(Long id, String summary, Double sentiment) {
        String summaryJson = summary == null ? "null" : "\"" + summary + "\"";
        return "{\"id\": %d, \"summary\": %s, \"sentiment\": %s}".formatted(id, summaryJson, sentiment);
    }

    private static String response(String... results) {
        return "{\"articles\": [" + String.join(", ", results) + "]}";
    }

    private Article createArticle(String externalId, String fullText) {
        return articleDAO.create(Article.builder()
                .externalId(externalId)
                .title("Title " + externalId)
                .fullText(fullText)
                .publishedAt(LocalDateTime.of(2026, 10, 20, 12, 0))
                .source(seed.source())
                .topic(seed.science())
                .build());
    }

    private Article reload(Article article) {
        return articleDAO.getById(article.getId());
    }

    @Test
    void summarizeBatch_updatesAllArticlesInBatch() {
        var service = serviceAnswering(response(
                result(seed.article1().getId(), "Planet found.", 0.6),
                result(seed.article2().getId(), "Chip announced.", -0.2)));

        assertEquals(2, service.summarizeBatch());

        assertEquals("Planet found.", reload(seed.article1()).getAiSummary());
        assertEquals(0.6, reload(seed.article1()).getSentimentScore());
        assertEquals("Chip announced.", reload(seed.article2()).getAiSummary());
        assertEquals(-0.2, reload(seed.article2()).getSentimentScore());
    }

    @Test
    void summarizeBatch_noArticlesToSummarize_returnsZeroWithoutCallingAi() {
        articleDAO.updateArticleSummaryAndSentiment(seed.article1().getId(), "Done", 0.0);
        articleDAO.updateArticleSummaryAndSentiment(seed.article2().getId(), "Done", 0.0);
        var service = serviceAnswering(response());

        assertEquals(0, service.summarizeBatch());
        assertTrue(prompts.isEmpty());
    }

    @Test
    void summarizeBatch_clampsSentimentToRange() {
        var service = serviceAnswering(response(
                result(seed.article1().getId(), "Very positive.", 3.5),
                result(seed.article2().getId(), "Very negative.", -2.0)));

        service.summarizeBatch();

        assertEquals(1.0, reload(seed.article1()).getSentimentScore());
        assertEquals(-1.0, reload(seed.article2()).getSentimentScore());
    }

    @Test
    void summarizeBatch_stripsWhitespaceFromSummary() {
        var service = serviceAnswering(response(
                result(seed.article1().getId(), "  Planet found.  ", 0.1)));

        service.summarizeBatch();

        assertEquals("Planet found.", reload(seed.article1()).getAiSummary());
    }

    @Test
    void summarizeBatch_ignoresUnknownId_andSavesTheRest() {
        var service = serviceAnswering(response(
                result(99999L, "Not in batch.", 0.5),
                result(seed.article1().getId(), "Planet found.", 0.6)));

        assertEquals(1, service.summarizeBatch());
        assertEquals("Planet found.", reload(seed.article1()).getAiSummary());
        assertNull(reload(seed.article2()).getAiSummary());
    }

    @Test
    void summarizeBatch_duplicateId_usesFirstResultOnly() {
        Long id = seed.article1().getId();
        var service = serviceAnswering(response(
                result(id, "First.", 0.1),
                result(id, "Second.", 0.9)));

        assertEquals(1, service.summarizeBatch());
        assertEquals("First.", reload(seed.article1()).getAiSummary());
        assertEquals(0.1, reload(seed.article1()).getSentimentScore());
    }

    @Test
    void summarizeBatch_incompleteResult_leavesThatArticleUntouched() {
        var service = serviceAnswering(response(
                result(seed.article1().getId(), "   ", 0.5),               // tomt resumé
                result(seed.article2().getId(), "Chip announced.", null)));  // ingen score

        assertEquals(0, service.summarizeBatch());
        assertNull(reload(seed.article1()).getAiSummary());
        assertNull(reload(seed.article2()).getAiSummary());
    }

    @Test
    void summarizeBatch_nullEntryInList_isIgnored() {
        var service = serviceAnswering(response(
                "null",
                result(seed.article1().getId(), "Planet found.", 0.6)));

        assertEquals(1, service.summarizeBatch());
    }

    @Test
    void summarizeBatch_invalidJson_throws502AndSavesNothing() {
        var service = serviceAnswering("```json {\"articles\": [");

        ApiException e = assertThrows(ApiException.class, service::summarizeBatch);
        assertEquals(502, e.getCode());
        assertNull(reload(seed.article1()).getAiSummary());
        assertNull(reload(seed.article2()).getAiSummary());
    }

    @Test
    void summarizeBatch_missingArticlesField_throws502() {
        var service = serviceAnswering("{}");

        ApiException e = assertThrows(ApiException.class, service::summarizeBatch);
        assertEquals(502, e.getCode());
    }

    @Test
    void summarizeBatch_aiError_isPassedOn() {
        var service = new ArticleSummarySentimentService(articleDAO, prompt -> {
            throw new ApiException(429, "Gemini rate limit reached");
        });

        ApiException e = assertThrows(ApiException.class, service::summarizeBatch);
        assertEquals(429, e.getCode());
        assertNull(reload(seed.article1()).getAiSummary());
    }

    @Test
    void summarizeBatch_usesJsonMode() {
        var service = new ArticleSummarySentimentService(articleDAO, new AiClient() {
            @Override
            public String generateText(String prompt) {
                throw new AssertionError("generateText was called, expected generateJson");
            }

            @Override
            public String generateJson(String prompt) {
                return response();
            }
        });

        assertEquals(0, service.summarizeBatch());
    }

    @Test
    void summarizeBatch_promptContainsIdsAndTitles() {
        serviceAnswering(response()).summarizeBatch();

        String prompt = prompts.getFirst();
        assertTrue(prompt.contains("\"id\":" + seed.article1().getId()));
        assertTrue(prompt.contains("\"id\":" + seed.article2().getId()));
        assertTrue(prompt.contains("Scientists find new planet"));
        assertTrue(prompt.contains("New chip announced"));
    }

    @Test
    void summarizeBatch_shortensLongTextTo1200Chars() {
        createArticle("test/long", "x".repeat(2000));

        serviceAnswering(response()).summarizeBatch();

        String prompt = prompts.getFirst();
        assertTrue(prompt.contains("x".repeat(1200)));
        assertFalse(prompt.contains("x".repeat(1201)));
    }

    @Test
    void summarizeBatch_articleWithoutFullText_doesNotCrash() {
        Article noText = createArticle("test/no-text", null);
        var service = serviceAnswering(response(result(noText.getId(), "Only a title.", 0.0)));

        assertEquals(1, service.summarizeBatch());
        assertEquals("Only a title.", reload(noText).getAiSummary());
    }

    @Test
    void summarizeBatch_sendsAtMostTenArticles() {
        for (int i = 0; i < 12; i++) {
            createArticle("test/batch-" + i, "Text " + i);
        }

        serviceAnswering(response()).summarizeBatch();

        long idsInPrompt = Pattern.compile("\"id\":\\d").matcher(prompts.getFirst()).results().count();
        assertEquals(10, idsInPrompt);
    }
}
