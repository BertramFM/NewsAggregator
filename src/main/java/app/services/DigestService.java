package app.services;

import app.daos.ArticleDAO;
import app.daos.DigestLogDAO;
import app.entities.Article;
import app.entities.DigestLog;
import app.entities.Frequency;
import app.entities.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

public class DigestService {

    private static Logger log = LoggerFactory.getLogger(DigestService.class);

    private static final int MaxArticlesPerDigest = 25;

    private final ArticleDAO articleDAO;
    private final DigestLogDAO digestLogDAO;
    private final AiClient aiClient;

    public DigestService(ArticleDAO articleDAO, DigestLogDAO digestLogDAO, AiClient aiClient) {
        this.articleDAO = articleDAO;
        this.digestLogDAO = digestLogDAO;
        this.aiClient = aiClient;
    }

    public static Period lastCompletedPeriod(Frequency frequency, LocalDateTime now){
        LocalDate today = now.toLocalDate();

        if (frequency == Frequency.DAILY) {
            return new Period(today.minusDays(1).atStartOfDay(), today.atStartOfDay());
        } else if (frequency == Frequency.WEEKLY) {
            LocalDate thisMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
            return new Period(thisMonday.minusWeeks(1).atStartOfDay(), thisMonday.atStartOfDay());
        }

        log.error("Unsupported frequency {}", frequency);
        throw new IllegalArgumentException("Unsupported frequency: " + frequency);
    }

    public DigestLog createDigest(Topic topic, Frequency frequency, LocalDateTime startDate, LocalDateTime endDate) {
        if (digestLogDAO.existByTopicAndFrequencyPeriod(topic.getId(), frequency, startDate)){
            log.debug("Digest already exists for {} {} {}", topic.getTopicName(), frequency, startDate);
            return null;
        }

        List<Article> articles = articleDAO.findByTopicAndPeriod(topic.getId(), startDate, endDate, MaxArticlesPerDigest);

        if (articles.isEmpty()){
            log.info("No articles found for topic \"{}\" | {}, {}", topic.getTopicName(), frequency, startDate);
            return null;
        }

        String text = aiClient.generate(buildPrompt(topic.getTopicName(), articles));

        DigestLog digest = DigestLog.builder()
                .digestText(text)
                .frequency(frequency)
                .periodStart(startDate)
                .periodEnd(endDate)
                .topic(topic)
                .articles(new HashSet<>(articles))
                .build();

        DigestLog saved = digestLogDAO.create(digest);
        log.info("Created {} digest for {} using {} articles", frequency, topic.getTopicName(), articles.size());
        return saved;
    }

    private static String buildPrompt(String topicName, List<Article> articles) {
        StringBuilder sb = new StringBuilder()
                .append("You are writing a short news digest about the topic ").append(topicName).append(". ")
                .append("Summarize the most important developments in 4-8 short bullet point, ")
                .append("in neutral language, based only on the articles below. Do not invent facts. \n\n");


        // Jeg skal måske ændre til snippet i stedet for fulltext eller måske bare en del af fulltextet såsom maks 500-600 words.
        for (Article a : articles) {
            sb.append("- ").append(a.getTitle());
            if (a.getFullText() != null && !a.getFullText().isBlank()) {
                sb.append(": ").append(a.getFullText());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public record Period(LocalDateTime startDate, LocalDateTime endDate){}
}
