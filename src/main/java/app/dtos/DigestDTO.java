package app.dtos;

import app.entities.DigestLog;
import app.entities.Frequency;

import java.time.LocalDateTime;

public record DigestDTO(Long id, String topicName, Frequency frequency,
                        LocalDateTime periodStart, LocalDateTime periodEnd,
                        LocalDateTime generatedAt, String digestText) {

    public DigestDTO(DigestLog digestLog) {
        this(digestLog.getId(),
                digestLog.getTopic().getTopicName(),
                digestLog.getFrequency(),
                digestLog.getPeriodStart(),
                digestLog.getPeriodEnd(),
                digestLog.getGeneratedAt(),
                digestLog.getDigestText());
    }
}
