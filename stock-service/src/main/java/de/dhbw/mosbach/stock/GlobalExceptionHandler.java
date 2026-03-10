package de.dhbw.mosbach.stock;

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
        int status = 500; // Internal Server Error default

        // Map standard exceptions to specific HTTP status codes
        if (e instanceof IllegalArgumentException || e instanceof BadRequestException) {
            status = 400; // Bad Request for invalid input
        } else if (e instanceof NotFoundException) {
            status = 404; // Not Found if stock symbol does not exist
        } else {
            e.printStackTrace(); // Log unexpected errors
        }

        String msg = e.getMessage() != null ? e.getMessage() : "Ein unbekannter Fehler ist aufgetreten.";

        // Build JSON payload manually to guarantee MediaType.APPLICATION_JSON
        String jsonPayload = String.format("{\"error\":\"%s\", \"message\":\"%s\"}", 
                                           e.getClass().getSimpleName(), 
                                           msg.replace("\"", "\\\""));

        return Response.status(status)
                .entity(jsonPayload)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}