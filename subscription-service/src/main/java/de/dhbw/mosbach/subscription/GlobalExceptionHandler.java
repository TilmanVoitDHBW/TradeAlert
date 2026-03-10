package de.dhbw.mosbach.subscription;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider 
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        int status = 500; // Default: Internal Server Error

        // Map standard exceptions to proper HTTP status codes
        if (e instanceof IllegalArgumentException || e instanceof BadRequestException) {
            status = 400; // 400 Bad Request
        } else if (e instanceof NotFoundException) {
            status = 404; // 404 Not Found
        } else {
            e.printStackTrace();
        }

        String msg = e.getMessage() != null ? e.getMessage() : "Ein unbekannter Fehler ist aufgetreten.";

        // Build JSON payload manually to enforce MediaType.APPLICATION_JSON
        // Prevents Quarkus from generating HTML error pages automatically
        String jsonPayload = String.format("{\"error\":\"%s\", \"message\":\"%s\"}", 
                                           e.getClass().getSimpleName(), 
                                           msg.replace("\"", "\\\""));

        return Response.status(status)
                .entity(jsonPayload)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}