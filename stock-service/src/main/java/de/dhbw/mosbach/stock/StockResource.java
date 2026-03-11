package de.dhbw.mosbach.stock;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Path("/stock")
@Produces(MediaType.APPLICATION_JSON)
public class StockResource {

    private static final Logger LOG = Logger.getLogger(StockResource.class);
    
    // Constant for fallback pricing when API limits are reached ($)
    private static final double DUMMY_PRICE = 150.00;

    // Demo prices
    private static final Map<String, Double> DEMO_PRICES = new ConcurrentHashMap<>(Map.of(
        "AAPL",  175.00,
        "GOOGL", 140.00,
        "AMZN",  185.00,
        "TSLA",  250.00
    ));

    @RestClient
    AlphaVantageClient client;

    @ConfigProperty(name="alphavantage.api-key")
    String apiKey;

    /** Returns a demo price that fluctuates ±1% each call. */
    private double getDemoPrice(String symbol) {
        double current = DEMO_PRICES.getOrDefault(symbol, DUMMY_PRICE);
        double change = 1 + (Math.random() * 0.02 - 0.01);
        double updated = current * change;
        DEMO_PRICES.put(symbol, updated);
        return updated;
    }

    /**
     * Fetches the current stock quote.
     * Uses GLOBAL_QUOTE for better compatibility with free API keys.
     */
    @GET
    @Path("/quote")
    @SuppressWarnings("unchecked")
    public StockQuote quote(@QueryParam("symbol") String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new BadRequestException("Bitte gib ein Aktiensymbol ein.");
        }
        
        // Basic length validation
        if (symbol.length() > 20) {
            throw new BadRequestException("Das Aktiensymbol ist zu lang.");
        }
        
        symbol = symbol.toUpperCase(Locale.ROOT);

        try {
            Map<String, Object> json = client.query("GLOBAL_QUOTE", symbol, null, apiKey);
            
            // Check if API limit is exceeded
            if (json.containsKey("Note") || json.containsKey("Information")) {
                LOG.warn("Alpha Vantage API Limit erreicht. Nutze Dummy-Kurs für " + symbol);
                return new StockQuote(symbol, getDemoPrice(symbol));
            }

            Map<String, Object> globalQuote = (Map<String, Object>) json.get("Global Quote");
            if (globalQuote == null || globalQuote.isEmpty()) {
                throw new NotFoundException("Aktie nicht gefunden");
            }

            double price = Double.parseDouble(globalQuote.get("05. price").toString());
            return new StockQuote(symbol, price);

        } catch (Exception e) {
            LOG.error("Quote Fehler: " + e.getMessage());
            // Fallback for network timeouts
            return new StockQuote(symbol, getDemoPrice(symbol));
        }
    }

    /**
     * Resolves a search query to a stock symbol.
     */
    @GET
    @Path("/resolve")
    @SuppressWarnings("unchecked")
    public StockResolve resolve(@QueryParam("query") String query) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Query fehlt");
        }
        String q = query.trim();

        try {
            Map<String, Object> json = client.search("SYMBOL_SEARCH", q, apiKey);
            
            if (json.containsKey("Note") || json.containsKey("Information")) {
                LOG.warn("Symbol Search blockiert. Nutze Query als Symbol.");
                return new StockResolve(q, q.toUpperCase(Locale.ROOT));
            }

            List<Map<String, Object>> bestMatches = (List<Map<String, Object>>) json.get("bestMatches");
            if (bestMatches == null || bestMatches.isEmpty()) {
                throw new NotFoundException("Keine Übereinstimmung gefunden");
            }

            Map<String, Object> firstMatch = bestMatches.get(0);
            String sym = firstMatch.get("1. symbol").toString().toUpperCase(Locale.ROOT);

            return new StockResolve(q, sym);

        } catch (Exception e) {
            LOG.error("Resolve Fehler: " + e.getMessage());
            return new StockResolve(q, q.toUpperCase(Locale.ROOT));
        }
    }
}