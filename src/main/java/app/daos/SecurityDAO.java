package app.daos;

import app.entities.Role;
import app.entities.User;
import app.exceptions.ApiException;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.util.List;

public class SecurityDAO implements ISecurityDAO {
    private static final String USER_ROLE = "USER";
    private static final String INVALID_LOGIN = "Forkert email eller password";

    private final EntityManagerFactory emf;

    public SecurityDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public UserDTO getVerifiedUser(String email, String password) {
        if (isBlankOrNull(email) || isBlankOrNull(password)) {
            throw new ApiException(400, INVALID_LOGIN);
        }

        try (EntityManager em = emf.createEntityManager()) {
            try {
                User user = findByEmail(em, normalizeEmail(email));
                if (user == null || !user.checkPassword(password)){
                    throw new ApiException(401, INVALID_LOGIN);
                }
                return new UserDTO(user.getEmail(), user.getRolesAsString());
            } catch (PersistenceException e) {
                throw new ApiException(500, "Failed to verify user: " + e.getMessage());
            }
        }
    }

    @Override
    public User createUser(String email, String userName, String password) {
        if (isBlankOrNull(email) || isBlankOrNull(password) || isBlankOrNull(userName)) {
            throw new ApiException(400, "Email, password, and user name are required");
        }

        String normalizedEmail = normalizeEmail(email);
        String strippedUserName = userName.strip();

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                if (findByEmail(em, normalizedEmail) != null) {
                    throw new ApiException(409, "Email er allerede i brug");
                }
                if (userNameExists(em, strippedUserName)) {
                    throw new ApiException(409, "Brugernavnet er allerede taget");
                }

                User user = User.builder()
                        .email(normalizedEmail)
                        .userName(strippedUserName)
                        .build();
                user.changePassword(password);
                user.addRole(findOrCreateRole(em, USER_ROLE));
                em.persist(user);
                em.getTransaction().commit();
                return user;
            } catch (PersistenceException e) {
                rollBack(em);
                throw new ApiException(500, "Failed to create user: " + e.getMessage());
            } catch (RuntimeException e) {
                rollBack(em);
                throw e;
            }
        }
    }

    @Override
    public Role createRole(String roleName){
        if (isBlankOrNull(roleName)) {
            throw new ApiException(400, "Role name is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                String upperCaseRoleName = roleName.strip().toUpperCase();
                if (em.find(Role.class, upperCaseRoleName) != null) {
                    throw new ApiException(409, "Role already exists");
                }

                Role role = new Role(upperCaseRoleName);
                em.persist(role);
                em.getTransaction().commit();
                return role;
            } catch (PersistenceException e) {
                rollBack(em);
                throw new ApiException(500, "Failed to create role: " + e.getMessage());
            } catch (RuntimeException e) {
                rollBack(em);
                throw e;
            }
        }
    }

    @Override
    public User addUserRole(String email, String roleName) {
        if (isBlankOrNull(email) || isBlankOrNull(roleName)) {
            throw new ApiException(400, "Email and role name are required");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                User user = findByEmail(em, normalizeEmail(email));
                if (user == null) {
                    throw new ApiException(404, "User not found");
                }
                Role role = em.find(Role.class, roleName.strip().toUpperCase());
                if (role == null) {
                    throw new ApiException(404, "Role not found");
                }
                user.addRole(role);
                em.getTransaction().commit();
                return user;
            } catch (PersistenceException e) {
                rollBack(em);
                throw new ApiException(500, "Failed to add role: " + e.getMessage());
            } catch (RuntimeException e) {
                rollBack(em);
                throw e;
            }
        }
    }


    private User findByEmail(EntityManager em, String email) {
        List<User> users = em.createQuery("SELECT u FROM User u " +
                "LEFT JOIN FETCH u.roles " +
                "WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultList();
        return users.isEmpty() ? null : users.get(0);
    }

    private boolean userNameExists(EntityManager em, String userName) {
        Long count = em.createQuery("SELECT COUNT(u) FROM User u " +
                "WHERE LOWER(u.userName) = LOWER(:userName)", Long.class)
                .setParameter("userName", userName)
                .getSingleResult();
        return count > 0;
    }

    private Role findOrCreateRole(EntityManager em, String roleName) {
        Role role = em.find(Role.class, roleName);
        if (role == null) {
            role = new Role(roleName);
            em.persist(role);
        }
        return role;
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase();
    }

    private static void rollBack(EntityManager em){
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }

    private static boolean isBlankOrNull(String str) {
        return str == null || str.isBlank();
    }
}
