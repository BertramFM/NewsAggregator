package app.dtos;

import app.entities.Frequency;
import app.entities.Subscription;

import java.time.LocalDateTime;

public record SubscriptionDTO(Long id, String topicName, Frequency frequency, LocalDateTime createdAt) {

    public SubscriptionDTO(Subscription subscription) {
        this(subscription.getId(),
                subscription.getTopic().getTopicName(),
                subscription.getFrequency(),
                subscription.getCreatedAt());
    }
}
