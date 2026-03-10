package de.dhbw.mosbach.orchestrator.dto;

public record QuoteResponse(String symbol, double priceUsd, String currency, double price) {}
