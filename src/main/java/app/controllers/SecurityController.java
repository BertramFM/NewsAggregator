package app.controllers;

import app.config.TokenSettings;
import app.daos.ISecurityDAO;
import app.dtos.LoginRequestDTO;
import app.dtos.RegisterRequestDTO;
import app.dtos.TokenResponseDTO;
import app.entities.User;
import app.exceptions.ApiException;
import app.server.AppRole;
import app.utils.Validator;
import dk.bugelhartmann.ITokenSecurity;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.UserDTO;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.security.RouteRole;

import java.util.Set;
import java.util.stream.Collectors;

public class SecurityController {
    private final ISecurityDAO securityDAO;
    private final TokenSettings tokenSettings;
    private final ITokenSecurity tokenSecurity = new TokenSecurity();

    public SecurityController(ISecurityDAO securityDAO, TokenSettings tokenSettings) {
        this.securityDAO = securityDAO;
        this.tokenSettings = tokenSettings;
    }


    public void register(Context ctx) {
        RegisterRequestDTO request = readRegisterRequest(ctx);

        String passwordError = Validator.validatePassword(request.password());
        if (passwordError != null) {
            throw new ApiException(400, passwordError);
        }

        User user = securityDAO.createUser(request.email(), request.userName(), request.password());
        UserDTO tokenUser = new UserDTO(user.getEmail(), user.getRolesAsString());

        ctx.status(HttpStatus.CREATED);
        ctx.json(new TokenResponseDTO(createToken(tokenUser), user.getEmail()));
    }

    public void login(Context ctx) {
        LoginRequestDTO request = readLoginRequest(ctx);

        UserDTO verifiedUser = securityDAO.getVerifiedUser(request.email(), request.password());

        ctx.json(new TokenResponseDTO(createToken(verifiedUser), verifiedUser.getUsername()));
    }

    public void authenticate(Context ctx) {
        if (HandlerType.OPTIONS.equals(ctx.method()) || isOpenEndpoint(ctx)) {
            return;
        }

        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Missing or broken Authorization header");
        }

        String token = header.substring("Bearer ".length());
        ctx.attribute("user", verifyToken(token));
    }

    public void authorize(Context ctx) {
        if (HandlerType.OPTIONS.equals(ctx.method()) || isOpenEndpoint(ctx)) {
            return;
        }
        UserDTO user = ctx.attribute("user");
        if (user == null) {
            throw new ApiException(401, "No user found from token");
        }
        Set<String> allowedRoles = ctx.routeRoles().stream()
                .map(role -> role.toString().toUpperCase())
                .collect(Collectors.toSet());

        boolean hasAllowedRole = user.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(role.toUpperCase()));

        if (!hasAllowedRole) {
            throw new ApiException(403, "User lacks required role: " + allowedRoles);
        }
    }

    private boolean isOpenEndpoint(Context ctx) {
        Set<RouteRole> roles = ctx.routeRoles();
        return roles.isEmpty() || roles.contains(AppRole.ANYONE);
    }

    private String createToken(UserDTO user) {
        try {
            return tokenSecurity.createToken(user, tokenSettings.issuer(),
                    tokenSettings.expireTimeMillis(), tokenSettings.secretKey());
        } catch (Exception e) {
            throw new ApiException(500, "Could not create token: " + e.getMessage());
        }
    }

    private UserDTO verifyToken(String token) {
        try {
            if (tokenSecurity.tokenIsValid(token, tokenSettings.secretKey()) && tokenSecurity.tokenNotExpired(token)) {
                return tokenSecurity.getUserWithRolesFromToken(token);
            }
        } catch (Exception e) {
        }
        throw new ApiException(401, "Invalid or expired token");
    }

    private RegisterRequestDTO readRegisterRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(RegisterRequestDTO.class);
        } catch (Exception e) {
            throw new ApiException(400, "Invalid request body");
        }
    }

    private LoginRequestDTO readLoginRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(LoginRequestDTO.class);
        } catch (Exception e) {
            throw new ApiException(400, "Invalid request body");
        }
    }
}
