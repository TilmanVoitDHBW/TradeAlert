package de.dhbw.mosbach.subscription;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.*;

@ApplicationScoped
public class SubscriptionRepository {

    @Transactional
    public void add(String userId, String symbol, Double upper, Double lower, Double entryPrice) {
        String sym = symbol.toUpperCase(Locale.ROOT);
        
        // Check if the user is already subscribed to this stock
        Subscription existing = Subscription.find("userId = ?1 and symbol = ?2", userId, sym).firstResult();
        
        if (existing != null) {
            existing.upperThresholdUsd = upper;
            existing.lowerThresholdUsd = lower;
            if (entryPrice != null) existing.entryPriceUsd = entryPrice;
        } else {
            Subscription sub = new Subscription(userId, sym, upper, lower, entryPrice);
            sub.persist();
        }
    }

    @Transactional
    public void updateThresholds(String userId, String symbol, Double upper, Double lower, Double currentPrice) {
        String sym = symbol.toUpperCase(Locale.ROOT);
        Subscription existing = Subscription.find("userId = ?1 and symbol = ?2", userId, sym).firstResult();
        
        if (existing == null) {
            throw new NotFoundException("Dieses Abonnement existiert nicht.");
        }

        // Prevent negative thresholds
        if (upper != null && upper < 0) {
            throw new IllegalArgumentException("Das obere Limit darf nicht negativ sein.");
        }
        if (lower != null && lower < 0) {
            throw new IllegalArgumentException("Das untere Limit darf nicht negativ sein.");
        }

        // Prevent logical errors: upper threshold below current price or lower threshold above current price
        if (currentPrice != null) {
            if (upper != null && upper <= currentPrice) {
                throw new IllegalArgumentException("Das obere Limit muss über dem aktuellen Kurs liegen.");
            }
            if (lower != null && lower >= currentPrice) {
                throw new IllegalArgumentException("Das untere Limit muss unter dem aktuellen Kurs iegen.");
            }
        } else if (upper != null || lower != null) {
            throw new IllegalArgumentException("Fehler: Das Backend benötigt den aktuellen Kurs, um die Limits zu validieren.");
        }

        existing.upperThresholdUsd = upper;
        existing.lowerThresholdUsd = lower;
    }

    @Transactional
    public void remove(String userId, String symbol) {
        String sym = symbol.toUpperCase(Locale.ROOT);
        Subscription.delete("userId = ?1 and symbol = ?2", userId, sym);
    }

    public List<Subscriptions.SubscriptionEntry> list(String userId) {
        List<Subscription> subs = Subscription.find("userId", userId).list();
        return subs.stream()
                .map(s -> new Subscriptions.SubscriptionEntry(s.symbol, s.upperThresholdUsd, s.lowerThresholdUsd, s.entryPriceUsd))
                .toList();
    }

    public Map<String, List<Subscriptions.SubscriptionEntry>> all() {
        List<Subscription> allSubs = Subscription.listAll();
        Map<String, List<Subscriptions.SubscriptionEntry>> out = new HashMap<>();
        
        for (Subscription s : allSubs) {
            out.computeIfAbsent(s.userId, k -> new ArrayList<>())
               .add(new Subscriptions.SubscriptionEntry(s.symbol, s.upperThresholdUsd, s.lowerThresholdUsd, s.entryPriceUsd));
        }
        return out;
    }
}