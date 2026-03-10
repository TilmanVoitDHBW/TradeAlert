package de.dhbw.mosbach.subscription;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Subscription extends PanacheEntity {
    public String userId;
    public String symbol;
    public Double upperThresholdUsd;
    public Double lowerThresholdUsd;
    public Double entryPriceUsd;

    // JPA requires a no-arg constructor
    public Subscription() {}

    public Subscription(String userId, String symbol, Double upperThresholdUsd, Double lowerThresholdUsd, Double entryPriceUsd) {
        this.userId = userId;
        this.symbol = symbol;
        this.upperThresholdUsd = upperThresholdUsd;
        this.lowerThresholdUsd = lowerThresholdUsd;
        this.entryPriceUsd = entryPriceUsd;
    }
}