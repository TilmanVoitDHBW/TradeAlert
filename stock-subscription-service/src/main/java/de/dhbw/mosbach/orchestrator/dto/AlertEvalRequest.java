package de.dhbw.mosbach.orchestrator.dto;

/**
 * DTO for sending threshold evaluation requests to the Alert-Service.
 */
public record AlertEvalRequest(
    String userId,
    String symbol,
    Double currentPriceUsd, 
    Double upperThresholdUsd,
    Double lowerThresholdUsd
) {}