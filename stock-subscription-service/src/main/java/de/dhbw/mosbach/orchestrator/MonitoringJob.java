package de.dhbw.mosbach.orchestrator;

import de.dhbw.mosbach.orchestrator.dto.*;
import de.dhbw.mosbach.orchestrator.Clients.*;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MonitoringJob {

    @RestClient
    SubscriptionClient subscription;

    @RestClient
    StockClient stock;

    @RestClient
    AlertClient alert;

    @Scheduled(cron = "{monitor.cron}")
    void monitor() {
        System.out.println("[MONITOR] Starting scheduled market evaluation...");
        Map<String, List<Subscriptions.SubscriptionEntry>> all = subscription.all();
        
        if (all == null || all.isEmpty()) {
            System.out.println("[MONITOR] No active subscriptions found. Skipping.");
            return;
        }

        for (Map.Entry<String, List<Subscriptions.SubscriptionEntry>> e : all.entrySet()) {
            String userId = e.getKey();
            if (userId == null || userId.isBlank()) continue;

            for (Subscriptions.SubscriptionEntry entry : e.getValue()) {
                if (entry != null && entry.symbol() != null && !entry.symbol().isBlank()) {
                    processSingleSubscription(userId, entry);
                }
            }
        }
        System.out.println("[MONITOR] Scheduled evaluation finished.");
    }

    /**
     * Handles the fetching and alerting for a single user subscription.
     */
    private void processSingleSubscription(String userId, Subscriptions.SubscriptionEntry entry) {
        StockQuote quote;
        try {
            quote = stock.quote(entry.symbol());
            System.out.println("[MONITOR] Fetched quote for " + entry.symbol() + ": $" + quote.priceUsd());
        } catch (Exception ex) {
            System.err.println("[MONITOR] Failed to fetch quote for " + entry.symbol() + ": " + ex.getMessage());
            return;
        }

        try {
            alert.evaluate(new AlertEvalRequest(
                userId, 
                entry.symbol(), 
                quote.priceUsd(), 
                entry.upperThresholdUsd(), 
                entry.lowerThresholdUsd()
            ));

            evaluateThresholdReset(userId, entry, quote.priceUsd());

        } catch (Exception ex) {
            System.err.println("[MONITOR] CRITICAL: Failed to process alert for " + entry.symbol() + ": " + ex.getMessage());
            ex.printStackTrace(); 
        }
    }

    /**
     * Checks if a threshold was breached and resets it to null if necessary (One-Cancels-Other logic).
     */
    private void evaluateThresholdReset(String userId, Subscriptions.SubscriptionEntry entry, double currentPrice) {
        Double currentUpper = entry.upperThresholdUsd();
        Double currentLower = entry.lowerThresholdUsd();
        boolean breached = false;

        if (currentUpper != null && currentPrice >= currentUpper) {
            currentUpper = null;
            breached = true;
            System.out.println("[MONITOR] Upper threshold breached for " + entry.symbol() + ". Resetting limit.");
        }
        
        if (currentLower != null && currentPrice <= currentLower) {
            currentLower = null;
            breached = true;
            System.out.println("[MONITOR] Lower threshold breached for " + entry.symbol() + ". Resetting limit.");
        }

        if (breached) {
            Map<String, Object> updatePayload = new HashMap<>();
            updatePayload.put("upperThresholdUsd", currentUpper);
            updatePayload.put("lowerThresholdUsd", currentLower);
            updatePayload.put("currentPriceUsd", currentPrice);
            
            subscription.updateThresholds(userId, entry.symbol(), updatePayload);
        }
    }
}