package app.daos;

import app.config.HibernateTestConfig;
import app.entities.Role;
import app.entities.User;
import app.exceptions.ApiException;
import app.testUtils.DatabaseTestPopulator;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityDAOTest {
    private static final String PASSWORD = "Hemmelig1!";

    private static EntityManagerFactory emf;
    private static SecurityDAO dao;

    @BeforeAll
    static void setUpAll() {
        emf = HibernateTestConfig.getEntityManagerFactory();
        dao = new SecurityDAO(emf);
    }

    @BeforeEach
    void setUp() {
        DatabaseTestPopulator.populate(emf);
    }

    @Test
    void createUser_normalizesEmailAndGivesUserRole() {
        User created = dao.createUser("  Anna@NewsSite.com ", " anna ", PASSWORD);

        assertNotNull(created.getId());
        assertEquals("anna@newssite.com", created.getEmail());
        assertEquals("anna", created.getUserName());
        assertEquals(Set.of("USER"), created.getRolesAsString());
    }

    @Test
    void createUser_hashesPassword() {
        User created = dao.createUser("anna@newssite.com", "anna", PASSWORD);

        assertNotEquals(PASSWORD, created.getPasswordHash());
        assertTrue(created.checkPassword(PASSWORD));
    }

    @Test
    void createUser_duplicateEmailWithOtherCase_returns409() {
        dao.createUser("anna@newssite.com", "anna", PASSWORD);

        ApiException ex = assertThrows(ApiException.class,
                () -> dao.createUser("ANNA@newssite.com", "anna2", PASSWORD));
        assertEquals(409, ex.getCode());
    }

    @Test
    void createUser_duplicateUserNameWithOtherCase_returns409() {
        dao.createUser("anna@newssite.com", "anna", PASSWORD);

        ApiException ex = assertThrows(ApiException.class,
                () -> dao.createUser("other@newssite.com", "ANNA", PASSWORD));
        assertEquals(409, ex.getCode());
    }

    @Test
    void createUser_blankField_returns400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> dao.createUser("anna@newssite.com", " ", PASSWORD));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createUser_twoUsersShareSameUserRole() {
        dao.createUser("anna@newssite.com", "anna", PASSWORD);
        dao.createUser("bo@newssite.com", "bo", PASSWORD);

        try (EntityManager em = emf.createEntityManager()) {
            Long roleCount = em.createQuery("SELECT COUNT(r) FROM Role r", Long.class).getSingleResult();
            assertEquals(1L, roleCount);
        }
    }

    @Test
    void getVerifiedUser_correctLogin_returnsEmailAndRoles() {
        dao.createUser("anna@newssite.com", "anna", PASSWORD);

        UserDTO verified = dao.getVerifiedUser("anna@newssite.com", PASSWORD);

        assertEquals("anna@newssite.com", verified.getUsername());
        assertEquals(Set.of("USER"), verified.getRoles());
    }

    @Test
    void getVerifiedUser_emailWithCapitalLetters_works() {
        dao.createUser("anna@newssite.com", "anna", PASSWORD);

        UserDTO verified = dao.getVerifiedUser(" Anna@NewsSite.com ", PASSWORD);

        assertEquals("anna@newssite.com", verified.getUsername());
    }

    @Test
    void getVerifiedUser_wrongPasswordAndUnknownEmail_sameMessage401() {
        dao.createUser("anna@newssite.com", "anna", PASSWORD);

        ApiException wrongPassword = assertThrows(ApiException.class,
                () -> dao.getVerifiedUser("anna@newssite.com", "Forkert1!"));
        ApiException unknownEmail = assertThrows(ApiException.class,
                () -> dao.getVerifiedUser("nobody@newssite.com", PASSWORD));

        assertEquals(401, wrongPassword.getCode());
        assertEquals(401, unknownEmail.getCode());
        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    @Test
    void createRole_storesUpperCaseName() {
        Role role = dao.createRole(" admin ");

        assertEquals("ADMIN", role.getRoleName());
    }

    @Test
    void createRole_duplicate_returns409() {
        dao.createRole("ADMIN");

        ApiException ex = assertThrows(ApiException.class, () -> dao.createRole("admin"));
        assertEquals(409, ex.getCode());
    }

    @Test
    void addUserRole_userGetsBothRoles() {
        dao.createUser("anna@newssite.com", "anna", PASSWORD);
        dao.createRole("ADMIN");

        dao.addUserRole("anna@newssite.com", "admin");

        UserDTO verified = dao.getVerifiedUser("anna@newssite.com", PASSWORD);
        assertEquals(Set.of("USER", "ADMIN"), verified.getRoles());
    }

    @Test
    void addUserRole_unknownUser_returns404() {
        dao.createRole("ADMIN");

        ApiException ex = assertThrows(ApiException.class,
                () -> dao.addUserRole("nobody@newssite.com", "ADMIN"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void addUserRole_unknownRole_returns404() {
        dao.createUser("anna@newssite.com", "anna", PASSWORD);

        ApiException ex = assertThrows(ApiException.class,
                () -> dao.addUserRole("anna@newssite.com", "EDITOR"));
        assertEquals(404, ex.getCode());
    }
}
