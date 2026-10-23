package app.dtos;

import app.entities.Article;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ArticleWithTextDTO {
    // Skipping id and externalId from the DTO setter
    @Setter(AccessLevel.NONE)
    private Long id;

    @Setter(AccessLevel.NONE)
    private String externalId;

    private String title;
    private String snippet;
    private String fullText;
    private LocalDateTime publishedAt;
    private String sourceUrl;
    private String sourceName;
    private String topicName;
    private Double sentimentScore;
    private String aiSummary;

    public ArticleWithTextDTO(Article article) {
        this.id = article.getId();
        this.externalId = article.getExternalId();
        this.title = article.getTitle();
        this.snippet = article.getSnippet();
        this.fullText = article.getFullText();
        this.publishedAt = article.getPublishedAt();
        this.sourceUrl = article.getSourceUrl();
        this.sourceName = article.getSource().getSourceName();

        this.topicName = article.getTopic().getTopicName();

        this.sentimentScore = article.getSentimentScore();
        this.aiSummary = article.getAiSummary();
    }
}
