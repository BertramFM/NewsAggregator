package app.daos;

import app.entities.DigestLog;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class DigestLogDAO implements GenericDAO<DigestLog, Long> {
    private final EntityManagerFactory emf;

    public DigestLogDAO(EntityManagerFactory emf){
        this.emf = emf;
    }

    @Override
    public DigestLog create(DigestLog digestLog) {
        if (digestLog == null) {
            throw new ApiException(400, "DigestLog is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.persist(digestLog);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Create DigestLog failed: " + e.getMessage());
            }
        }
        return digestLog;
    }

    @Override
    public DigestLog getById(Long id) {
        if (id == null) {
            throw new ApiException(400, "DigestLog id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                return em.find(DigestLog.class, id);
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get DigestLog failed: " + e.getMessage());
            }
        }
    }

    @Override
    public List<DigestLog> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<DigestLog> query = em.createQuery("SELECT f FROM DigestLog f", DigestLog.class);
                return query.getResultList();
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get DigestLog failed: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            throw new ApiException(400, "DigestLog id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Long> query = em.createQuery(
                        "SELECT COUNT(a) FROM DigestLog a WHERE a.id = :id", Long.class);
                query.setParameter("id", id);
                return query.getSingleResult() > 0;
            } catch (PersistenceException e) {
                throw new ApiException(500, "Check DigestLog existence failed: " + e.getMessage());
            }
        }
    }

    @Override
    public DigestLog update(DigestLog DigestLog) {

        if (DigestLog == null || DigestLog.getId() == null) {
            throw new ApiException(400, "Movie id is required");
        }

        DigestLog updated;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                DigestLog existing = em.find(DigestLog.class, DigestLog.getId());
                if (existing == null) {
                    throw new ApiException(404, "Movie not found");
                }
                updated = em.merge(DigestLog);
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
                DigestLog DigestLogToRemove = em.find(DigestLog.class, id);
                if (DigestLogToRemove != null) {
                    em.remove(DigestLogToRemove);
                } else {
                    throw new ApiException(404, "DigestLog not found");
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
