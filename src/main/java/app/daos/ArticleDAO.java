package app.daos;

import app.entities.Article;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class ArticleDAO implements GenericDAO<Article, Long> {
    private final EntityManagerFactory emf;

    public ArticleDAO(EntityManagerFactory emf){
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
                return em.find(Article.class, id);
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get Article failed: " + e.getMessage());
            }
        }
    }

    @Override
    public List<Article> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Article> query = em.createQuery("SELECT f FROM Article f", Article.class);
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
    public Article update(Article Article) {

        if (Article == null || Article.getId() == null) {
            throw new ApiException(400, "Movie id is required");
        }

        Article updated;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Article existing = em.find(Article.class, Article.getId());
                if (existing == null) {
                    throw new ApiException(404, "Movie not found");
                }
                updated = em.merge(Article);
                em.getTransaction().commit();
            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Update Movie failed: " + e.getMessage());
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
            throw new ApiException(400, "Movie id is required");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Article ArticleToRemove = em.find(Article.class, id);
                if (ArticleToRemove != null) {
                    em.remove(ArticleToRemove);
                } else {
                    throw new ApiException(404, "Article not found");
                }
                em.getTransaction().commit();
            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Delete Movie failed: " + e.getMessage());
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
        return true;
    }
}
