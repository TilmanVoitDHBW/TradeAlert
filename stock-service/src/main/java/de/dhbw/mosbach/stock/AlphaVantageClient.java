package de.dhbw.mosbach.stock;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@RegisterRestClient(configKey = "alphavantage")
@Path("/query")
@Produces(MediaType.APPLICATION_JSON)
public interface AlphaVantageClient {

    @GET
    Map<String, Object> query(@QueryParam("function") String function,
                             @QueryParam("symbol") String symbol,
                             @QueryParam("interval") String interval,
                             @QueryParam("apikey") String apiKey);

    @GET
    Map<String, Object> search(@QueryParam("function") String function,
                               @QueryParam("keywords") String keywords,
                               @QueryParam("apikey") String apiKey);
}