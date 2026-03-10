package de.dhbw.mosbach.subscription;

import java.util.List;

public record Subscriptions(String userId, List<SubscriptionEntry> entries) {
    public record SubscriptionEntry(String symbol, Double upperThresholdUsd, Double lowerThresholdUsd, Double entryPriceUsd) {}
}