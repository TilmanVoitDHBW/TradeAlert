package de.dhbw.mosbach.currency;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Locale;
import java.util.Map;

@Path("/currency")
@Produces(MediaType.APPLICATION_JSON)
public class CurrencyResource {

    @RestClient
    CurrencyClient client;

    /** * Converts a given USD amount into a requested target currency. 
     */
    @GET
    @Path("/convert")
    public Map<String, Object> convert(@QueryParam("amountUsd") double amountUsd,
                                       @QueryParam("to") String to) {
                                           
        if (!Double.isFinite(amountUsd) || amountUsd < 0) {
            throw new BadRequestException("Der angegebene Betrag (USD) ist ungültig.");
        }
        if (to == null || to.isBlank()) {
            throw new BadRequestException("Die Zielwährung fehlt.");
        }
        
        String code = to.toLowerCase(Locale.ROOT);

        // Fetch live exchange rates
        Map<String, Object> json = client.usd();
        Object usdObj = json.get("usd");
        
        if (!(usdObj instanceof Map<?,?> usdMap)) {
            throw new NotFoundException("Unerwartete API-Antwort beim Währungsabruf.");
        }

        // Extract the specific rate for the requested currency
        Object rateObj = usdMap.get(code);
        if (rateObj == null) {
            throw new NotFoundException("Unbekannte Zielwährung: " + code);
        }

        double rate;
        try { 
            rate = Double.parseDouble(rateObj.toString()); 
        } catch (NumberFormatException e) { 
            throw new NotFoundException("Ungültiger Wechselkurs empfangen."); 
        }

        double converted = amountUsd * rate;
        
        return Map.of(
            "from", "usd", 
            "to", code, 
            "rate", rate, 
            "convertedAmount", converted
        );
    }
}