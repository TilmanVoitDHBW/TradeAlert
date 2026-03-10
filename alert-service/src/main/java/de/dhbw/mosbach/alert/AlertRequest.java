package de.dhbw.mosbach.alert;

/**
 * Request DTO for alert evaluation.
 */
public record AlertRequest(
    String userId, 
    String symbol, 
    double currentPriceUsd, 
    Double upperThresholdUsd, 
    Double lowerThresholdUsd
) {}