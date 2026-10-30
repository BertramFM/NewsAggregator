package app.controllers;

import app.daos.DigestLogDAO;
import app.dtos.DigestDTO;
import app.exceptions.ApiException;
import dk.bugelhartmann.UserDTO;
import io.javalin.http.Context;

import java.util.List;

public class DigestController {
    private final DigestLogDAO digestLogDAO;

    public DigestController(DigestLogDAO digestLogDAO) {
        this.digestLogDAO = digestLogDAO;
    }

    public void getMine(Context ctx) {
        List<DigestDTO> digests = digestLogDAO.findForUser(currentUserEmail(ctx)).stream()
                .map(DigestDTO::new)
                .toList();
        ctx.json(digests);
    }

    private String currentUserEmail(Context ctx) {
        UserDTO user = ctx.attribute("user");
        if (user == null) {
            throw new ApiException(401, "No user found from token");
        }
        return user.getUsername();
    }
}
