package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String snippet;

    @Column(columnDefinition = "TEXT")
    private String fullText;

    @Column()
    private String country;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Column()
    private String sourceUrl;

    @Column()
    private Double sentimentScore;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;


    // ======== Relations ========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "article_topics",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private List<Topic> topics = new ArrayList<>();
    // ===========================

    public void addTopic(Topic topic) {
        topics.add(topic);
        topic.getArticles().add(this);
    }

    public void removeTopic(Topic topic) {
        topics.remove(topic);
        topic.getArticles().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
