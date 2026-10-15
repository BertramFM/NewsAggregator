package app.daos;

import app.entities.ApiProvider;
import app.entities.Source;
import app.entities.Topic;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class TopicDAO implements GenericDAO<Topic, Long> {
    private final EntityManagerFactory emf;

    public TopicDAO(EntityManagerFactory emf){
        this.emf = emf;
    }

    @Override
    public Topic create(Topic topic) {
        if (topic == null) {
            throw new ApiException(400, "Topic is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.persist(topic);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Create Topic failed: " + e.getMessage());
            }
        }
        return topic;
    }

    @Override
    public Topic getById(Long id) {
        if (id == null) {
            throw new ApiException(400, "Topic id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                return em.find(Topic.class, id);
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get Topic failed: " + e.getMessage());
            }
        }
    }

    @Override
    public List<Topic> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Topic> query = em.createQuery("SELECT f FROM Topic f", Topic.class);
                return query.getResultList();
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get Topic failed: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            throw new ApiException(400, "Topic id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Long> query = em.createQuery(
                        "SELECT COUNT(a) FROM Topic a WHERE a.id = :id", Long.class);
                query.setParameter("id", id);
                return query.getSingleResult() > 0;
            } catch (PersistenceException e) {
                throw new ApiException(500, "Check Topic existence failed: " + e.getMessage());
            }
        }
    }

    @Override
    public Topic update(Topic Topic) {

        if (Topic == null || Topic.getId() == null) {
            throw new ApiException(400, "Movie id is required");
        }

        Topic updated;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                Topic existing = em.find(Topic.class, Topic.getId());
                if (existing == null) {
                    throw new ApiException(404, "Movie not found");
                }
                updated = em.merge(Topic);
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
                Topic TopicToRemove = em.find(Topic.class, id);
                if (TopicToRemove != null) {
                    em.remove(TopicToRemove);
                } else {
                    throw new ApiException(404, "Topic not found");
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

    public Topic findOrCreate(String name) {
        if (name == null || name.isEmpty()) {
            throw new ApiException(400, "Topic name is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                List<Topic> found = em.createQuery(
                                "SELECT t FROM Topic t WHERE t.topicName = :name", Topic.class)
                        .setParameter("name", name)
                        .getResultList();

                Topic topic;
                if (found.isEmpty()){
                    topic = Topic.builder()
                            .topicName(name)
                            .build();
                    em.persist(topic);
                } else {
                    topic = found.getFirst();
                }
                em.getTransaction().commit();
                return topic;
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Find or create Topic failed: " + e.getMessage());
            }
        }
    }
}
