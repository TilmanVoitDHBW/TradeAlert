package de.dhbw.mosbach.orchestrator.dto;

import java.time.Instant;

public record Alert(String userId, String symbol, double previousPriceUsd, double currentPriceUsd,
                    double percentChange, String message, Instant createdAt) {}
