package app.services;

import app.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ArticleSummarySentimentSchedule {
    private static final Logger log = LoggerFactory.getLogger(ArticleSummarySentimentSchedule.class);

    private static final int MAX_BATCHES_PER_RUN = 5;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ArticleSummarySentimentService service;

    public ArticleSummarySentimentSchedule(ArticleSummarySentimentService articleSummarySentimentService) {
        this.service = articleSummarySentimentService;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::run, 3, 15, TimeUnit.MINUTES);
    }

    public void run() {
        try {
            for (int i = 0; i < MAX_BATCHES_PER_RUN; i++) {
                int updated = service.summarizeBatch();
                if (updated == 0) {
                    break;
                }
            }
        } catch (ApiException e) {
            if (e.getCode() == 429) {
                log.info("API rate limit exceeded, will try summarizing again later.");
            } else {
                log.warn("Error summarizing articles: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error running summary schedule: {}", e.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
