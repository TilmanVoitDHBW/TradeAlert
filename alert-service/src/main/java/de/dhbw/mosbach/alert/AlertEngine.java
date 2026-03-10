package de.dhbw.mosbach.alert;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AlertEngine {

    // In-memory state to prevent duplicate alerts
    public static class ThresholdState {
        public Double lastPriceUsd;
        public Double lastUpperAlerted; 
        public Double lastLowerAlerted; 
        public Double activeUpperThreshold;
        public Double activeLowerThreshold;
        
        public ThresholdState() {} 
    }

    private final Map<String, ThresholdState> states = new ConcurrentHashMap<>();

    @Transactional
    public List<Alert> evaluate(String userId, String symbol, double currentUsd, Double upperThresholdUsd, Double lowerThresholdUsd) {
        
        if (currentUsd <= 0.0) {
            return Collections.emptyList();
        }
        
        String sym = symbol.toUpperCase(Locale.ROOT);
        String key = userId + "|" + sym;
        
        ThresholdState state = states.computeIfAbsent(key, k -> new ThresholdState());
        List<Alert> triggeredAlerts = new ArrayList<>();

        // Reset alert state if thresholds change
        if (!Objects.equals(state.activeUpperThreshold, upperThresholdUsd)) {
            state.lastUpperAlerted = null; 
            state.activeUpperThreshold = upperThresholdUsd;
        }
        if (!Objects.equals(state.activeLowerThreshold, lowerThresholdUsd)) {
            state.lastLowerAlerted = null; 
            state.activeLowerThreshold = lowerThresholdUsd;
        }

        Double prevUsd = state.lastPriceUsd;

        // Check upper limit
        if (upperThresholdUsd != null && currentUsd >= upperThresholdUsd) {
            if (!Objects.equals(state.lastUpperAlerted, upperThresholdUsd)) {
                String msg = String.format(Locale.ROOT, "OBERES LIMIT ERREICHT: %s hat den oberen Schwellenwert erreicht!", sym);
                
                Alert alert = new Alert(userId, sym, prevUsd != null ? prevUsd : currentUsd, currentUsd, 0.0, msg, Instant.now());
                alert.persist(); 
                
                triggeredAlerts.add(alert);
                state.lastUpperAlerted = upperThresholdUsd; 
            }
        }

        // Check lower limit
        if (lowerThresholdUsd != null && currentUsd <= lowerThresholdUsd) {
            if (!Objects.equals(state.lastLowerAlerted, lowerThresholdUsd)) {
                String msg = String.format(Locale.ROOT, "UNTERES LIMIT ERREICHT: %s hat den unteren Schwellenwert erreicht!", sym);
                
                Alert alert = new Alert(userId, sym, prevUsd != null ? prevUsd : currentUsd, currentUsd, 0.0, msg, Instant.now());
                alert.persist(); 
                
                triggeredAlerts.add(alert);
                state.lastLowerAlerted = lowerThresholdUsd; 
            }
        }

        state.lastPriceUsd = currentUsd;
        return triggeredAlerts;
    }

    @Transactional
    public List<Alert> list(String userId, boolean clear) {
        List<Alert> userAlerts = Alert.list("userId", userId);
        
        if (clear && !userAlerts.isEmpty()) {
            Alert.delete("userId", userId);
        }
        return userAlerts;
    }

    @Transactional
    public void removeSingleAlert(String userId, String createdAtStr) {
        Alert.delete("userId = ?1 and createdAt = ?2", userId, Instant.parse(createdAtStr));
    }

    public void removeStockState(String userId, String symbol) {
        String sym = symbol.toUpperCase(Locale.ROOT);
        states.remove(userId + "|" + sym);
    }
}