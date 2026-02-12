package com.notification.lib.config;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a notification provider.
 * Holds credentials and connection details needed to authenticate
 * with provider APIs (SendGrid, Twilio, Firebase, etc.).
 *
 * <p><b>Security note:</b> API keys and secrets should never be hardcoded.
 * Load them from environment variables or a secrets manager.</p>
 *
 * <p>Uses Builder pattern for clean, fluent configuration.</p>
 */
@Builder
public class ProviderConfig {

    /**
     * Primary API key or account identifier.
     * Examples: SendGrid API Key, Twilio Account SID, Firebase Server Key.
     */
    @Getter
    private final String apiKey;

    /**
     * Secondary secret/token for providers that require dual authentication.
     * Example: Twilio Auth Token.
     */
    @Getter
    private final String apiSecret;

    /**
     * Domain or region for the provider.
     * Example: Mailgun domain ("mg.example.com"), AWS region ("us-east-1").
     */
    @Getter
    private final String domain;

    /**
     * Custom properties for provider-specific configuration.
     * Examples: Firebase projectId, custom endpoints, webhook URLs.
     *
     * <p>Returns an unmodifiable view of the properties map to preserve encapsulation.</p>
     */
    @Builder.Default
    private final Map<String, String> properties = new HashMap<>();

    /**
     * Returns an unmodifiable view of the custom properties map.
     * Prevents external mutation of internal state.
     */
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Convenience method to create a simple config with just an API key.
     */
    public static ProviderConfig withApiKey(String apiKey) {
        return ProviderConfig.builder().apiKey(apiKey).build();
    }
}
