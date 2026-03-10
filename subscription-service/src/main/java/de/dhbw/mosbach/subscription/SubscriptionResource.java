package de.dhbw.mosbach.subscription;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubscriptionResource {

    private final SubscriptionRepository repo;

    public SubscriptionResource(SubscriptionRepository repo) {
        this.repo = repo;
    }

    @POST
    public Map<String, Object> subscribe(Subscription sub) {
        if (sub == null || sub.userId == null || sub.userId.isBlank()) {
            throw new BadRequestException("User-ID fehlt");
        }
        if (sub.symbol == null || sub.symbol.isBlank()) {
            throw new BadRequestException("Aktie fehlt");
        }
        
        repo.add(sub.userId, sub.symbol, sub.upperThresholdUsd, sub.lowerThresholdUsd, sub.entryPriceUsd);
        return Map.of("status", "ok");
    }

    /**
     * DTO for receiving threshold updates from the frontend.
     */
    public static class ThresholdUpdate {
        public Double upperThresholdUsd;
        public Double lowerThresholdUsd;
        public Double currentPriceUsd; // Included to validate limits against the live price
    }

    @PUT
    @Path("/{userId}/{symbol}")
    public Map<String, Object> updateThresholds(@PathParam("userId") String userId, @PathParam("symbol") String symbol, ThresholdUpdate req) {
        repo.updateThresholds(userId, symbol, req.upperThresholdUsd, req.lowerThresholdUsd, req.currentPriceUsd);
        return Map.of("status", "updated");
    }

    @GET
    @Path("/{userId}")
    public Subscriptions list(@PathParam("userId") String userId) {
        return new Subscriptions(userId, repo.list(userId));
    }

    @DELETE
    @Path("/{userId}/{symbol}")
    public void unsubscribe(@PathParam("userId") String userId, @PathParam("symbol") String symbol) {
        repo.remove(userId, symbol);
    }

    @GET
    public Map<String, List<Subscriptions.SubscriptionEntry>> all() {
        return repo.all();
    }
}