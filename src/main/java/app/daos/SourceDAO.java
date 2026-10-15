package app.daos;

import app.entities.ApiProvider;
import app.entities.Source;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class SourceDAO implements GenericDAO<Source, Long> {
    private final EntityManagerFactory emf;

    public SourceDAO(EntityManagerFactory emf){
        this.emf = emf;
    }

    @Override
    public Source create(Source source) {
        if (source == null) {
            throw new ApiException(400, "Source is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.persist(source);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Create Source failed: " + e.getMessage());
            }
        }
        return source;
    }

    @Override
    public Source getById(Long id) {
        if (id == null) {
            throw new ApiException(400, "Source id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                return em.find(Source.class, id);
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get Source failed: " + e.getMessage());
            }
        }
    }

    @Override
    public List<Source> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Source> query = em.createQuery("SELECT f FROM Source f", Source.class);
                return query.getResultList();
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get Source failed: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            throw new ApiException(400, "Source id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Long> query = em.createQuery(
                        "SELECT COUNT(a) FROM Source a WHERE a.id = :id", Long.class);
                query.setParameter("id", id);
                return query.getSingleResult() > 0;
            } catch (PersistenceException e) {
                throw new ApiException(500, "Check Source existence failed: " + e.getMessage());
            }
        }
    }

    @Override
    public Source update(Source Source) {

        if (Source == null || Source.getId() == null) {
            throw new ApiException(400, "Movie id is required");
        }

        Source updated;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Source existing = em.find(Source.class, Source.getId());
                if (existing == null) {
                    throw new ApiException(404, "Movie not found");
                }
                updated = em.merge(Source);
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
                Source SourceToRemove = em.find(Source.class, id);
                if (SourceToRemove != null) {
                    em.remove(SourceToRemove);
                } else {
                    throw new ApiException(404, "Source not found");
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

    public Source findOrCreate(String name, ApiProvider apiProvider, String url) {
        if (name == null || name.isEmpty() || apiProvider == null || url == null || url.isEmpty()) {
            throw new ApiException(400, "Source name and provider is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                List<Source> found = em.createQuery(
                        "SELECT s FROM Source s WHERE s.sourceName = :name AND s.apiProvider = :provider", Source.class)
                        .setParameter("name", name)
                        .setParameter("provider", apiProvider)
                        .getResultList();

                Source source;
                if (found.isEmpty()){
                    source = Source.builder()
                            .sourceName(name)
                            .apiProvider(apiProvider)
                            .url(url)
                            .build();
                    em.persist(source);
                } else {
                    source = found.getFirst();
                }
                em.getTransaction().commit();
                return source;
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Find or create Source failed: " + e.getMessage());
            }
        }
    }


}
