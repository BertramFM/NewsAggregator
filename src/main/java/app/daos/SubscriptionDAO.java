package app.daos;

import app.entities.Frequency;
import app.entities.Subscription;
import app.entities.Topic;
import app.entities.User;
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

    public Subscription subscribe(String email, String topicName, Frequency frequency) {
        if (email == null || topicName == null || topicName.isBlank() || frequency == null) {
            throw new ApiException(400, "Email, topic name and frequency are required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                User user = findUserByEmail(em, email);
                if (user == null) {
                    throw new ApiException(404, "User not found");
                }
                Topic topic = findTopicByName(em, topicName.strip().toLowerCase());
                if (topic == null) {
                    throw new ApiException(404, "Emnet findes ikke: " + topicName.strip());
                }
                if (isSubscribed(em, user.getId(), topic.getId())) {
                    throw new ApiException(409, "Du abonnerer allerede på dette emne");
                }
                Subscription subscription = Subscription.builder()
                        .user(user)
                        .topic(topic)
                        .frequency(frequency)
                        .build();
                em.persist(subscription);
                em.getTransaction().commit();
                return subscription;
            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Subscribe failed: " + e.getMessage());
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    public List<Subscription> findByUserEmail(String email) {
        if (email == null) {
            throw new ApiException(400, "Email is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                return em.createQuery(
                                "SELECT s FROM Subscription s JOIN FETCH s.topic t " +
                                "WHERE s.user.email = :email ORDER BY t.topicName", Subscription.class)
                        .setParameter("email", email)
                        .getResultList();
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get subscriptions for user failed: " + e.getMessage());
            }
        }
    }

    public void deleteForUser(String topicName, String email) {
        if (topicName == null || topicName.isBlank() || email == null) {
            throw new ApiException(400, "Topic name and email are required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                List<Subscription> found = em.createQuery(
                                "SELECT s FROM Subscription s " +
                                "WHERE s.topic.topicName = :topicName AND s.user.email = :email",
                                Subscription.class)
                        .setParameter("topicName", topicName.strip().toLowerCase())
                        .setParameter("email", email)
                        .getResultList();
                if (found.isEmpty()) {
                    throw new ApiException(404, "Du abonnerer ikke på emnet: " + topicName.strip());
                }
                em.remove(found.get(0));
                em.getTransaction().commit();
            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Delete subscription for user failed: " + e.getMessage());
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    private User findUserByEmail(EntityManager em, String email) {
        List<User> users = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultList();
        return users.isEmpty() ? null : users.get(0);
    }

    private Topic findTopicByName(EntityManager em, String topicName) {
        List<Topic> topics = em.createQuery("SELECT t FROM Topic t WHERE t.topicName = :name", Topic.class)
                .setParameter("name", topicName)
                .getResultList();
        return topics.isEmpty() ? null : topics.get(0);
    }

    private boolean isSubscribed(EntityManager em, Long userId, Long topicId) {
        Long count = em.createQuery(
                        "SELECT COUNT(s) FROM Subscription s WHERE s.user.id = :userId AND s.topic.id = :topicId",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("topicId", topicId)
                .getSingleResult();
        return count > 0;
    }
}
