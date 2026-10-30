package app.controllers;

import app.daos.SubscriptionDAO;
import app.dtos.SubscriptionDTO;
import app.dtos.SubscriptionRequestDTO;
import app.entities.Frequency;
import app.entities.Subscription;
import app.exceptions.ApiException;
import dk.bugelhartmann.UserDTO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class SubscriptionController {
    private final SubscriptionDAO subscriptionDAO;

    public SubscriptionController(SubscriptionDAO subscriptionDAO) {
        this.subscriptionDAO = subscriptionDAO;
    }

    public void create(Context ctx) {
        SubscriptionRequestDTO request = readSubscriptionRequest(ctx);
        Frequency frequency = parseFrequency(request.frequency());

        Subscription subscription = subscriptionDAO.subscribe(currentUserEmail(ctx), request.topicName(), frequency);

        ctx.status(HttpStatus.CREATED);
        ctx.json(new SubscriptionDTO(subscription));
    }

    public void getMine(Context ctx) {
        List<SubscriptionDTO> subscriptions = subscriptionDAO.findByUserEmail(currentUserEmail(ctx)).stream()
                .map(SubscriptionDTO::new)
                .toList();
        ctx.json(subscriptions);
    }

    public void delete(Context ctx) {
        String topicName = ctx.pathParam("topicName");
        subscriptionDAO.deleteForUser(topicName, currentUserEmail(ctx));
        ctx.json(Map.of(
                "message", "Subscription deleted",
                "topicName", topicName
        ));
    }

    private String currentUserEmail(Context ctx) {
        UserDTO user = ctx.attribute("user");
        if (user == null) {
            throw new ApiException(401, "No user found from token");
        }
        return user.getUsername();
    }

    private Frequency parseFrequency(String frequency) {
        if (frequency == null || frequency.isBlank()) {
            throw new ApiException(400, "Frekvens skal udfyldes (DAILY eller WEEKLY)");
        }
        try {
            return Frequency.valueOf(frequency.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "Frekvens skal være DAILY eller WEEKLY");
        }
    }

    private SubscriptionRequestDTO readSubscriptionRequest(Context ctx) {
        try {
            return ctx.bodyAsClass(SubscriptionRequestDTO.class);
        } catch (Exception e) {
            throw new ApiException(400, "Invalid request body");
        }
    }
}
