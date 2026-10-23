package app.daos;

import app.entities.User;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class UserDAO implements GenericDAO<User, Long> {
    private final EntityManagerFactory emf;

    public UserDAO(EntityManagerFactory emf){
        this.emf = emf;
    }

    @Override
    public User create(User user) {
        if (user == null) {
            throw new ApiException(400, "User is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.persist(user);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Create User failed: " + e.getMessage());
            }
        }
        return user;
    }

    @Override
    public User getById(Long id) {
        if (id == null) {
            throw new ApiException(400, "User id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                return em.find(User.class, id);
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get User failed: " + e.getMessage());
            }
        }
    }

    @Override
    public List<User> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<User> query = em.createQuery("SELECT f FROM User f", User.class);
                return query.getResultList();
            } catch (PersistenceException e) {
                throw new ApiException(500, "Get User failed: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            throw new ApiException(400, "User id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                TypedQuery<Long> query = em.createQuery(
                        "SELECT COUNT(a) FROM User a WHERE a.id = :id", Long.class);
                query.setParameter("id", id);
                return query.getSingleResult() > 0;
            } catch (PersistenceException e) {
                throw new ApiException(500, "Check User existence failed: " + e.getMessage());
            }
        }
    }

    @Override
    public User update(User user) {

        if (user == null || user.getId() == null) {
            throw new ApiException(400, "User id is required");
        }

        User updated;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                User existing = em.find(User.class, user.getId());
                if (existing == null) {
                    throw new ApiException(404, "User not found");
                }
                updated = em.merge(user);
                em.getTransaction().commit();
            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Update User failed: " + e.getMessage());
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
            throw new ApiException(400, "User id is required");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                User UserToRemove = em.find(User.class, id);
                if (UserToRemove != null) {
                    em.remove(UserToRemove);
                } else {
                    throw new ApiException(404, "User not found");
                }
                em.getTransaction().commit();
            } catch (PersistenceException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new ApiException(500, "Delete User failed: " + e.getMessage());
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
