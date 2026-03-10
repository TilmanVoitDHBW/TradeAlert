package de.dhbw.mosbach.orchestrator;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        // Handle exceptions thrown by downstream backend services
        if (e instanceof WebApplicationException webEx) {
            Response r = webEx.getResponse();
            try {
                r.bufferEntity(); // IMPORTANT: Buffer the entity to prevent stream read crashes
                String errorJson = r.readEntity(String.class); 
                
                return Response.status(r.getStatus())
                        .entity(errorJson) 
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            } catch (Exception ignored) {}
        }

        // Fallback for internal orchestrator errors
        return Response.status(500)
                .entity("{\"error\": \"Internal Error\", \"message\": \"" + e.getMessage() + "\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}