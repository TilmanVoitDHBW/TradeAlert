package de.dhbw.mosbach.orchestrator.dto;

import java.util.List;

/**
 * Aggregated subscription data for a user.
 */
public record Subscriptions(String userId, List<SubscriptionEntry> entries) {
    
    public record SubscriptionEntry(
            String symbol, 
            Double upperThresholdUsd, 
            Double lowerThresholdUsd, 
            Double entryPriceUsd
    ) {}
}