package app.daos;

import app.entities.Subscription;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class SubscriptionDAO implements GenericDAO<Subscription, Long> {
    private final EntityManagerFactory emf;

    public SubscriptionDAO(EntityManagerFactory emf){
        this.emf = emf;
    }

    @Override
    public Subscription create(Subscription subscription) {
        if (subscription == null) {
            throw new ApiException(400, "Subscription is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.persist(subscription);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Create Subscription failed: " + e.getMessage());
            }
        }
        return subscription;
    }

    @Override
    public Subscription getById(Long id) {
        if (id == null) {
            throw new ApiException(400, "Subscription id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                return em.find(Subscription.class, id);
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get Subscription failed: " + e.getMessage());
            }
        }
    }

    @Override
    public List<Subscription> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Subscription> query = em.createQuery("SELECT f FROM Subscription f", Subscription.class);
                return query.getResultList();
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get Subscription failed: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            throw new ApiException(400, "Subscription id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Long> query = em.createQuery(
                        "SELECT COUNT(a) FROM Subscription a WHERE a.id = :id", Long.class);
                query.setParameter("id", id);
                return query.getSingleResult() > 0;
            } catch (PersistenceException e) {
                throw new ApiException(500, "Check Subscription existence failed: " + e.getMessage());
            }
        }
    }

    @Override
    public Subscription update(Subscription subscription) {

        if (subscription == null || subscription.getId() == null) {
            throw new ApiException(400, "Subscription id is required");
        }

        Subscription updated;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Subscription existing = em.find(Subscription.class, subscription.getId());
                if (existing == null) {
                    throw new ApiException(404, "Subscription not found");
                }
                updated = em.merge(subscription);
                em.getTransaction().commit();
            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Update Subscription failed: " + e.getMessage());
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
            throw new ApiException(400, "Subscription id is required");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Subscription SubscriptionToRemove = em.find(Subscription.class, id);
                if (SubscriptionToRemove != null) {
                    em.remove(SubscriptionToRemove);
                } else {
                    throw new ApiException(404, "Subscription not found");
                }
                em.getTransaction().commit();
            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Delete Subscription failed: " + e.getMessage());
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
