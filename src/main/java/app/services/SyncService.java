package app.services;

import app.daos.ArticleDAO;
import app.daos.SourceDAO;
import app.daos.TopicDAO;
import app.dtos.GuardianApiResponseDTO;
import app.dtos.GuardianArticleDTO;
import app.dtos.GuardianResponseDTO;
import app.entities.ApiProvider;
import app.entities.Article;
import app.entities.Source;
import app.entities.Topic;
import app.mappers.GuardianArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class SyncService {
    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private static final int MaxPagesPerTopic = 5;

    private final ArticleDAO articleDAO;
    private final TopicDAO topicDAO;
    private final SourceDAO sourceDAO;
    private final GuardianUrlBuilder guardianUrlBuilder;
    private final APIReader apiReader;

    public SyncService(ArticleDAO articleDAO, TopicDAO topicDAO, SourceDAO sourceDAO,
                       GuardianUrlBuilder guardianUrlBuilder, APIReader apiReader) {
        this.articleDAO = articleDAO;
        this.topicDAO = topicDAO;
        this.sourceDAO = sourceDAO;
        this.guardianUrlBuilder = guardianUrlBuilder;
        this.apiReader = apiReader;
    }

    public int syncFromDate(LocalDate fromDate) {
        Source source = sourceDAO.findOrCreate("The Guardian", ApiProvider.GUARDIAN, "https://www.theguardian.com");

        int totalSaved = 0;

        for (GuardianTopic guardianTopic : GuardianTopic.values()) {

            try {
                Topic topic = topicDAO.findOrCreate(guardianTopic.getSectionId());
                int saved = syncTopic(guardianTopic, topic, source, fromDate);
                totalSaved += saved;
                log.info("Saved {} articles from topic {}", saved, guardianTopic);
            } catch (RuntimeException e) {
                log.error("Error syncing topic: '{}' | {}", guardianTopic, e.getMessage());
            }
        }
        log.info("Sync finished. Total saved: {}", totalSaved);
        return totalSaved;
    }

    private int syncTopic(GuardianTopic guardianTopic, Topic topic, Source source, LocalDate fromDate) {
        int saved = 0;
        int notSaved = 0;
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages && page <= MaxPagesPerTopic) {
            String url = guardianUrlBuilder.buildURLFromDateTopic(guardianTopic, fromDate, page);
            GuardianApiResponseDTO wrapper = apiReader.get(url, GuardianApiResponseDTO.class);
            GuardianResponseDTO response = wrapper.getResponse();

            totalPages = response.getPages();

            if (response.getResults() != null) {
                for (GuardianArticleDTO articleDTO : response.getResults()){
                    if (saveIfNew(articleDTO, source, topic)) {
                        saved++;
                    } else {
                        notSaved++;
                    }
                }
                log.info("Did not save {} articles from topic {} on page {}", notSaved, guardianTopic, page);
            }
            page++;
        }
        return saved;
    }

    private boolean saveIfNew(GuardianArticleDTO articleDTO, Source source, Topic topic){
        // filters out non-articles
        if (!"article".equals(articleDTO.getType())){
            log.info("Skipping since not an article: {} | {}", articleDTO.getId(), articleDTO.getType());
            return false;
        }
        // filters out articles without body text
        if (articleDTO.getFields() == null || articleDTO.getFields().getBodyText() == null || articleDTO.getFields().getBodyText().isEmpty()){
            log.info("Article has no body text: {} | {}", articleDTO.getId(), articleDTO.getType());
            return false;
        }
        // checks if article already exists in db using Guardian apis provided article id
        if (articleDAO.existsByExternalId(articleDTO.getId())){
            log.info("Article already exists: {} | {}", articleDTO.getId(), articleDTO.getType());
            return false;
        }

        try {
            Article article = GuardianArticleMapper.toEntity(articleDTO, source, topic);
            articleDAO.create(article);
            return true;
        } catch (Exception e){
            log.error("Error saving article: {} | {}", articleDTO.getId(), e.getMessage());
            return false;
        }
    }


}
