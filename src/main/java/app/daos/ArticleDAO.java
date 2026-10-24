package app.daos;

import app.entities.Article;
import app.entities.DigestLog;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

public class ArticleDAO implements GenericDAO<Article, Long> {
    private final EntityManagerFactory emf;

    public ArticleDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Article create(Article article) {
        if (article == null) {
            throw new ApiException(400, "Article is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.persist(article);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Create Article failed: " + e.getMessage());
            }
        }
        return article;
    }

    @Override
    public Article getById(Long id) {
        if (id == null) {
            throw new ApiException(400, "Article id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                return em.createQuery("""
                SELECT a FROM Article a
                JOIN FETCH a.source
                JOIN FETCH a.topic
                WHERE a.id = :id
                """, Article.class).setParameter("id", id)
                        .getResultStream()
                        .findFirst()
                        .orElse(null);
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get Article failed: " + e.getMessage());
            }
        }
    }

    @Override
    public List<Article> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Article> query = em.createQuery("""
                SELECT a FROM Article a
                JOIN FETCH a.source
                JOIN FETCH a.topic
                ORDER BY a.publishedAt DESC
                """, Article.class);
                return query.getResultList();
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get Article failed: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            throw new ApiException(400, "Article id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Long> query = em.createQuery(
                        "SELECT COUNT(a) FROM Article a WHERE a.id = :id", Long.class);
                query.setParameter("id", id);
                return query.getSingleResult() > 0;
            } catch (PersistenceException e) {
                throw new ApiException(500, "Check Article existence failed: " + e.getMessage());
            }
        }
    }


    public boolean existsByExternalId(String externalId) {
        if (externalId == null) {
            throw new ApiException(400, "External id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Long> query = em.createQuery(
                        "SELECT COUNT(a) FROM Article a WHERE a.externalId = :externalId", Long.class);
                query.setParameter("externalId", externalId);
                return query.getSingleResult() > 0;
            } catch (PersistenceException e) {
                throw new ApiException(500, "Check Article existence failed: " + e.getMessage());
            }
        }
    }

    @Override
    public Article update(Article article) {

        if (article == null || article.getId() == null) {
            throw new ApiException(400, "Article id is required");
        }

        Article updated;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Article existing = em.find(Article.class, article.getId());
                if (existing == null) {
                    throw new ApiException(404, "Article not found");
                }
                updated = em.merge(article);
                em.getTransaction().commit();
            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Update Article failed: " + e.getMessage());
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
        return updated;
    }

    @Override
    public boolean delete(Long id) {

        if (id == null) {
            throw new ApiException(400, "Article id is required");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Article articleToRemove = em.find(Article.class, id);

                if (articleToRemove == null) {
                    throw new ApiException(404, "Article not found");
                }

                List<DigestLog> digestLogs = em.createQuery(
                        "SELECT d FROM DigestLog d JOIN d.articles a WHERE a.id = :id", DigestLog.class)
                        .setParameter("id", id)
                        .getResultList();

                for (DigestLog digestLog : digestLogs) {
                    digestLog.getArticles().remove(articleToRemove);
                }
                em.remove(articleToRemove);
                em.getTransaction().commit();

            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Delete Article failed: " + e.getMessage());
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
        return true;
    }

    public LocalDateTime findLatestPublishedAt(Long topicId) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                return em.createQuery(
                        "SELECT MAX(a.publishedAt) FROM Article a WHERE a.topic.id = :topicId",
                        LocalDateTime.class)
                        .setParameter("topicId", topicId)
                        .getSingleResult();
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get publishedAt failed: " + e.getMessage());
            }
        }
    }

    public List<Article> findByTopicAndPeriod(Long topicId, LocalDateTime periodStart,
                                              LocalDateTime periodEnd, int limit) {
        if (topicId == null || periodStart == null || periodEnd == null || limit <= 0) {
            throw new ApiException(400, "Topic id, Period start and end is required. Limit must be a positive integer");
        }

        try (EntityManager em = emf.createEntityManager()) {
            try {
                return em.createQuery("" +
                        "SELECT a FROM Article a " +
                        "WHERE a.topic.id = :topicId " +
                        "AND a.publishedAt >= :start AND a.publishedAt < :end " +
                        "ORDER BY a.publishedAt DESC", Article.class)
                        .setParameter("topicId", topicId)
                        .setParameter("start", periodStart)
                        .setParameter("end", periodEnd)
                        .setMaxResults(limit)
                        .getResultList();
            } catch (PersistenceException e) {
                throw new ApiException(500, "Finding articles by topic and period failed: " + e.getMessage());
            }
        }
    }
}
