package de.dhbw.mosbach.alert;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import java.time.Instant;

@Entity
public class Alert extends PanacheEntity {
    
    public String userId;
    public String symbol;
    public double previousPriceUsd;
    public double currentPriceUsd;
    public double percentChange;
    public String message;
    public Instant createdAt;

    // JPA requires a no-arg constructor
    public Alert() {}

    public Alert(String userId, String symbol, double previousPriceUsd, double currentPriceUsd,
                 double percentChange, String message, Instant createdAt) {
        this.userId = userId;
        this.symbol = symbol;
        this.previousPriceUsd = previousPriceUsd;
        this.currentPriceUsd = currentPriceUsd;
        this.percentChange = percentChange;
        this.message = message;
        this.createdAt = createdAt;
    }
}