package de.dhbw.mosbach.orchestrator.dto;

public record SubscribeRequest(
    String userId,
    String query,
    Double upperThresholdUsd,
    Double lowerThresholdUsd
) {}
