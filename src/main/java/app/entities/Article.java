package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
    @ToString.Exclude
    private Source source;

//    @Builder.Default
//    @ManyToMany(fetch = FetchType.LAZY)
//    @JoinTable(name = "article_topics",
//            joinColumns = @JoinColumn(name = "article_id"),
//            inverseJoinColumns = @JoinColumn(name = "topic_id"))
//    @ToString.Exclude
//    private Set<Topic> topics = new HashSet<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    @ToString.Exclude
    private Topic topic;

    // ===========================

//    public void addTopic(Topic topic) {
//        topics.add(topic);
//        topic.getArticles().add(this);
//    }
//
//    public void removeTopic(Topic topic) {
//        topics.remove(topic);
//        topic.getArticles().remove(this);
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
