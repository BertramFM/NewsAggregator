package app.testUtils;

import app.entities.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class DatabaseTestPopulator {
    private DatabaseTestPopulator() {}

    public record SeedData(Source source, Topic science, Topic technology,
                           Article article1, Article article2,
                           User user, DigestLog digestLog) {}

    public static SeedData populate(EntityManagerFactory emf) {
        Source guardian = Source.builder()
                .sourceName("The Guardian")
                .apiProvider(ApiProvider.GUARDIAN)
                .url("https://www.theguardian.com")
                .build();

        Topic science = Topic.builder().topicName("science").build();
        Topic technology = Topic.builder().topicName("technology").build();

        Article article1 = Article.builder()
                .externalId("test/article1")
                .title("Scientists find new planet")
                .snippet("Kort uddrag 1")
                .fullText("Fuld tekst 1")
                .publishedAt(LocalDateTime.of(2026, 10, 18, 12, 0))
                .sourceUrl("https://example.com/1")
                .source(guardian)
                .topic(science)
                .build();

        Article article2 = Article.builder()
                .externalId("test/article2")
                .title("New chip announced")
                .snippet("Kort uddrag 2")
                .fullText("Fuld tekst 2")
                .publishedAt(LocalDateTime.of(2026, 10, 19, 12, 0))
                .sourceUrl("https://example.com/2")
                .source(guardian)
                .topic(technology)
                .build();

        User user = User.builder()
                .email("test@newssite.com")
                .passwordHash("not-a-real-hash")
                .userName("tester")
                .build();

        DigestLog digestLog = DigestLog.builder()
                .digestText("Test-digest")
                .frequency(Frequency.DAILY)
                .periodStart(LocalDateTime.of(2026, 10, 13, 0, 0))
                .periodEnd(LocalDateTime.of(2026, 10, 20, 0, 0))
                .topic(science)
                .articles(new HashSet<>(Set.of(article1, article2)))
                .build();

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.createNativeQuery(
                        "TRUNCATE TABLE digest_articles, digest_logs, subscriptions, users, "
                                + "articles, topics, sources RESTART IDENTITY CASCADE").executeUpdate();

                em.persist(guardian);
                em.persist(science);
                em.persist(technology);
                em.persist(article1);
                em.persist(article2);
                em.persist(user);
                em.persist(digestLog);

                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
        return new SeedData(guardian, science, technology, article1, article2, user, digestLog);
    }
}