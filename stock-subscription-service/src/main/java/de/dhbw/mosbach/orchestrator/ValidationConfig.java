package de.dhbw.mosbach.orchestrator;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.regex.Pattern;

@ApplicationScoped
public class ValidationConfig {

    @ConfigProperty(name="validation.query.max-length", defaultValue="50")
    int maxLen;

    // allow letters, digits and spaces (no special characters)
    @ConfigProperty(name="validation.query.pattern", defaultValue="^[A-Za-z0-9 ]+$")
    String pattern;

    public int maxLen() { return maxLen; }
    public Pattern pattern() { return Pattern.compile(pattern); }
}
