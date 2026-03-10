package de.dhbw.mosbach.orchestrator;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import de.dhbw.mosbach.orchestrator.dto.*;

public class Clients {

    @RegisterRestClient(configKey="subscription")
    @Path("/subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public interface SubscriptionClient {
        @POST
        Map<String, Object> subscribe(Map<String, Object> req);

        @PUT
        @Path("/{userId}/{symbol}")
        Map<String, Object> updateThresholds(@PathParam("userId") String userId, @PathParam("symbol") String symbol, Map<String, Object> req);

        @GET
        @Path("/{userId}")
        Subscriptions list(@PathParam("userId") String userId);

        @GET
        Map<String, List<Subscriptions.SubscriptionEntry>> all();

        @DELETE
        @Path("/{userId}/{symbol}")
        void unsubscribe(@PathParam("userId") String userId, @PathParam("symbol") String symbol);
    }

    @RegisterRestClient(configKey="stock")
    @Path("/stock")
    @Produces(MediaType.APPLICATION_JSON)
    public interface StockClient {
        @GET
        @Path("/quote")
        StockQuote quote(@QueryParam("symbol") String symbol);

        @GET
        @Path("/resolve")
        StockResolve resolve(@QueryParam("query") String query);
    }

    @RegisterRestClient(configKey="currency")
    @Path("/currency")
    @Produces(MediaType.APPLICATION_JSON)
    public interface CurrencyClient {
        @GET
        @Path("/convert")
        Map<String, Object> convert(@QueryParam("amountUsd") double amountUsd, @QueryParam("to") String to);
    }

    @RegisterRestClient(configKey="alert")
    @Path("/alerts")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public interface AlertClient {
        @POST
        @Path("/evaluate")
        Map<String, Object> evaluate(AlertEvalRequest req);

        @GET
        List<Alert> list(@QueryParam("userId") String userId, @QueryParam("clear") boolean clear);

        // Allow the orchestrator to delete a single alert in the alert-service
        @DELETE
        @Path("/{userId}/single")
        void deleteSingleAlert(@PathParam("userId") String userId, @QueryParam("createdAt") String createdAt);

        // Clear internal alert state for a specific subscription
        @DELETE
        @Path("/state/{userId}/{symbol}")
        void deleteStockState(@PathParam("userId") String userId, @PathParam("symbol") String symbol);
    }
}