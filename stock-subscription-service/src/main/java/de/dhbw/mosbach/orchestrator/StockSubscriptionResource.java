package de.dhbw.mosbach.orchestrator;

import de.dhbw.mosbach.orchestrator.dto.*;
import de.dhbw.mosbach.orchestrator.Clients.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StockSubscriptionResource {

    private static final double exchangeFallbackRate = 0.92;

    @RestClient
    StockClient stock;

    @RestClient
    SubscriptionClient subscription;

    @RestClient
    CurrencyClient currencyClient;

    @RestClient
    AlertClient alertClient;

    @jakarta.inject.Inject
    PriceCache priceCache;

    @GET
    @Path("/exchange-rate")
    public Map<String, Object> getExchangeRate() {
        try {
            return currencyClient.convert(1.0, "eur");
        } catch (Exception e) {
            return Map.of("convertedAmount", exchangeFallbackRate);
        }
    }

    @POST
    @Path("/subscribe")
    public Map<String, Object> subscribe(SubscribeRequest req) {
        StockQuote quote;
        try { 
            quote = stock.quote(req.query()); 
        } catch (Exception e) { 
            throw new NotFoundException("Aktie existiert nicht."); 
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", req.userId());
        payload.put("symbol", req.query());
        payload.put("upperThresholdUsd", req.upperThresholdUsd());
        payload.put("lowerThresholdUsd", req.lowerThresholdUsd());
        payload.put("entryPriceUsd", quote.priceUsd());

        return subscription.subscribe(payload);
    }

    @GET
    @Path("/subscriptions/{userId}")
    public Map<String, Object> getSubscriptions(@PathParam("userId") String userId) {
        Subscriptions subs = subscription.list(userId);
        
        List<Map<String, Object>> enrichedEntries = new ArrayList<>();
        for (var entry : subs.entries()) {
            Map<String, Object> item = new HashMap<>();
            item.put("symbol", entry.symbol());
            item.put("upperThresholdUsd", entry.upperThresholdUsd());
            item.put("lowerThresholdUsd", entry.lowerThresholdUsd());
            
            Double entryPrice = entry.entryPriceUsd() != null ? entry.entryPriceUsd() : 0.0;
            item.put("entryPriceUsd", entryPrice);

            item.put("currentPriceUsd", priceCache.get(entry.symbol()));
            enrichedEntries.add(item);
        }

        return Map.of("userId", userId, "entries", enrichedEntries);
    }

    @PUT
    @Path("/subscriptions/{userId}/{symbol}")
    public Map<String, Object> updateThresholds(@PathParam("userId") String userId, @PathParam("symbol") String symbol, Map<String, Object> req) {
        return subscription.updateThresholds(userId, symbol, req);
    }

    @DELETE
    @Path("/subscriptions/{userId}/{symbol}")
    public Map<String, String> unsubscribe(@PathParam("userId") String userId, @PathParam("symbol") String symbol) {
        subscription.unsubscribe(userId, symbol);
        
        // Also notify the alert service to clear its state cache for this symbol
        try {
            alertClient.deleteStockState(userId, symbol);
        } catch (Exception ignored) {
            // Ignore if the alert service is temporarily unavailable
        }
        
        return Map.of("status", "deleted", "symbol", symbol);
    }

    @GET
    @Path("/quote")
    public Map<String, Object> quote(@QueryParam("query") String query) {
        try {
            StockQuote quote = stock.quote(query);
            return Map.of("symbol", query.toUpperCase(), "priceUsd", quote.priceUsd());
        } catch (Exception e) {
            throw new NotFoundException("Aktie nicht gefunden: " + query);
        }
    }

    @GET
    @Path("/alerts")
    public List<Alert> getAlerts(@QueryParam("userId") String userId, @QueryParam("clear") @DefaultValue("false") boolean clear) {
        return alertClient.list(userId, clear);
    }

    @DELETE
    @Path("/alerts/{userId}/single")
    public void deleteSingleAlert(@PathParam("userId") String userId, @QueryParam("createdAt") String createdAt) {
        alertClient.deleteSingleAlert(userId, createdAt);
    }
}