package app.server;

import io.javalin.security.RouteRole;

public enum AppRole implements RouteRole {
    ANYONE,
    USER,
    ADMIN
}
