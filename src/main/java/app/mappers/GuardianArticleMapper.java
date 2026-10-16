package app.mappers;

import app.dtos.GuardianArticleDTO;
import app.entities.Article;
import app.entities.Source;
import app.entities.Topic;

import java.time.ZoneOffset;

public class GuardianArticleMapper {

    private static final int snippetLength = 300;

    public static Article toEntity(GuardianArticleDTO dto, Source source, Topic topic) {

        String bodyText = (dto.getFields() != null) ? dto.getFields().getBodyText() : null;

        Article article = Article.builder()
                .externalId(dto.getId())
                .title(dto.getWebTitle())
                .snippet(createSnippet(bodyText))
                .fullText(bodyText)
                .publishedAt(dto.getWebPublicationDate().withOffsetSameInstant(ZoneOffset.UTC). toLocalDateTime())
                .sourceUrl(dto.getWebUrl())
                .source(source)
                .build();

        article.getTopics().add(topic);

        return article;
    }

    public static String createSnippet(String bodyText) {
        if (bodyText == null || bodyText.isBlank()) return null;

        String trimmed = bodyText.strip();

        return trimmed.length() <= snippetLength ?
                trimmed :
                trimmed.substring(0, snippetLength) + "...";
    }

}
