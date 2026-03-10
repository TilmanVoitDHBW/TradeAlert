package de.dhbw.mosbach.currency;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * REST client to fetch live currency exchange rates.
 */
@RegisterRestClient(configKey="currency")
@Path("/npm/@fawazahmed0/currency-api@latest/v1/currencies")
@Produces(MediaType.APPLICATION_JSON)
public interface CurrencyClient {

    @GET
    @Path("/usd.json")
    Map<String, Object> usd();
}