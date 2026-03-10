package de.dhbw.mosbach.orchestrator;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PriceCache {

    private final Map<String, Double> prices = new ConcurrentHashMap<>();

    public void put(String symbol, Double price) {
        if (symbol != null && price != null) prices.put(symbol.toUpperCase(), price);
    }

    public Double get(String symbol) {
        return symbol != null ? prices.get(symbol.toUpperCase()) : null;
    }
}