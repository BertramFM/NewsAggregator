package app.testUtils;

import app.entities.ApiProvider;
import app.entities.Article;
import app.entities.Source;
import app.entities.Topic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;

public class DatabaseTestPopulator {
    private DatabaseTestPopulator() {}

    public record SeedData(Source source, Topic science, Topic technology,
                           Article article1, Article article2) {}

    public static SeedData populate(EntityManagerFactory emf) {
        Source guardian = Source.builder()
                .sourceName("The Guardian")
                .apiProvider(ApiProvider.GUARDIAN)
                .url("https://www.theguardian.com")
                .build();

        Topic science = Topic.builder().topicName("science").build();
        Topic technology = Topic.builder().topicName("technology").build();

        Article article1 = Article.builder()
                .externalId("test/article-1")
                .title("Scientists find new planet")
                .snippet("Kort snippet 1")
                .fullText("Fuld tekst 1")
                .publishedAt(LocalDateTime.of(2026, 10, 18, 12, 0))
                .sourceUrl("https://theguardian.com/1")
                .source(guardian)
                .topic(science)
                .build();

        Article article2 = Article.builder()
                .externalId("test/article-2")
                .title("New chip announced")
                .snippet("Kort snippet 2")
                .fullText("Fuld tekst 2")
                .publishedAt(LocalDateTime.of(2026, 10, 19, 12, 0))
                .sourceUrl("https://theguardian.com/2")
                .source(guardian)
                .topic(technology)
                .build();

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.createNativeQuery(
                        "TRUNCATE TABLE digest_articles, digest_logs, articles, topics, sources "
                                + "RESTART IDENTITY CASCADE").executeUpdate();

                em.persist(guardian);
                em.persist(science);
                em.persist(technology);
                em.persist(article1);
                em.persist(article2);

                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
        return new SeedData(guardian, science, technology, article1, article2);
    }
}