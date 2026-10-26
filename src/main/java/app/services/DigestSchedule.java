package app.services;

import app.daos.TopicDAO;
import app.entities.Frequency;
import app.entities.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DigestSchedule {
    private static final Logger log = LoggerFactory.getLogger(DigestSchedule.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DigestService digestService;
    private final TopicDAO topicDAO;

    public DigestSchedule(DigestService digestService, TopicDAO topicDAO) {
        this.digestService = digestService;
        this.topicDAO = topicDAO;
    }

    public void start(){
        scheduler.scheduleWithFixedDelay(this::run, 1, 60, TimeUnit.MINUTES);
    }

    protected void run(){
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            for (Frequency frequency : Frequency.values()) {
                DigestService.Period period = DigestService.lastCompletedPeriod(frequency, now);

                for (Topic topic : topicDAO.findTopicsWithSubscribedUsers(frequency)) {
                    try {
                        digestService.createDigest(topic, frequency, period.startDate(), period.endDate());
                    } catch (Exception e) {
                        log.warn("Error creating digest for {} {}: {}", topic.getTopicName(), frequency, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error running digest schedule: {}", e.getMessage());
        }
    }

    public void stop(){
        scheduler.shutdown();
    }
}
