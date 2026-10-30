package app.daos;

import app.entities.Role;
import app.entities.User;
import dk.bugelhartmann.UserDTO;

public interface ISecurityDAO {
    UserDTO getVerifiedUser(String email, String password);
    User createUser(String email, String userName, String password);
    Role createRole(String roleName);
    User addUserRole(String email, String roleName);
}
