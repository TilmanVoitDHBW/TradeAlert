package de.dhbw.mosbach.alert;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertResource {

    private final AlertEngine engine;

    public AlertResource(AlertEngine engine) {
        this.engine = engine;
    }

    @POST
    @Path("/evaluate")
    public Map<String, Object> evaluate(AlertRequest req) {
        if (req == null || req.userId() == null || req.userId().isBlank()) {
            throw new BadRequestException("User-ID fehlt");
        }
        if (req.symbol() == null || req.symbol().isBlank()) {
            throw new BadRequestException("Aktie fehlt");
        }
        
        List<Alert> newAlerts = engine.evaluate(
                req.userId(), 
                req.symbol(), 
                req.currentPriceUsd(), 
                req.upperThresholdUsd(), 
                req.lowerThresholdUsd()
        );
        
        if (!newAlerts.isEmpty()) {
            return Map.of("notify", true, "alerts", newAlerts);
        }
        
        return Map.of("notify", false);
    }

    @GET
    public List<Alert> list(@QueryParam("userId") String userId,
                            @QueryParam("clear") @DefaultValue("false") boolean clear) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("User-ID fehlt");
        }
        return engine.list(userId, clear);
    }

    @DELETE
    @Path("/{userId}/single")
    public void deleteSingleAlert(@PathParam("userId") String userId, @QueryParam("createdAt") String createdAt) {
        engine.removeSingleAlert(userId, createdAt);
    }

    @DELETE
    @Path("/state/{userId}/{symbol}")
    public void deleteStockState(@PathParam("userId") String userId, @PathParam("symbol") String symbol) {
        engine.removeStockState(userId, symbol);
    }
}