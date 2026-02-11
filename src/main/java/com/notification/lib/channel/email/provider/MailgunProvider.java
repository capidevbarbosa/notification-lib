package com.notification.lib.channel.email.provider;

import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.config.ProviderConfig;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.SendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Simulated Mailgun email provider.
 *
 * <p>In a real implementation, this would use Mailgun's Messages API
 * (POST https://api.mailgun.net/v3/{domain}/messages) with:
 * - Basic Auth: api:{API_KEY}
 * - Multipart form data with from, to, subject, text/html fields.</p>
 *
 * <p>The simulation logs the operation and returns a realistic response
 * matching Mailgun's format (id and message fields).</p>
 */
public class MailgunProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(MailgunProvider.class);
    private static final String PROVIDER_NAME = "Mailgun";

    private final ProviderConfig config;

    public MailgunProvider(ProviderConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ProviderConfig must not be null");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("API key is required for Mailgun provider");
        }
        if (config.getDomain() == null || config.getDomain().isBlank()) {
            throw new IllegalArgumentException("Domain is required for Mailgun provider");
        }
        this.config = config;
    }

    @Override
    public NotificationResult send(EmailMessage message) {
        log.info("[Mailgun] Sending email to '{}' with subject '{}'", message.getRecipient(), message.getSubject());
        log.debug("[Mailgun] Domain: {}, From: {}", config.getDomain(), message.getFrom());

        try {
            // Simulate API call to Mailgun
            // POST https://api.mailgun.net/v3/{domain}/messages
            simulateApiCall(message);

            String messageId = "<" + UUID.randomUUID().toString().substring(0, 12) + "@" + config.getDomain() + ">";
            log.info("[Mailgun] Email sent successfully. Message ID: {}", messageId);

            return NotificationResult.success(messageId, PROVIDER_NAME, ChannelType.EMAIL);

        } catch (Exception e) {
            log.error("[Mailgun] Failed to send email to '{}': {}", message.getRecipient(), e.getMessage());
            throw new SendException("Mailgun failed to send email: " + e.getMessage(), PROVIDER_NAME, e);
        }
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    private void simulateApiCall(EmailMessage message) {
        log.debug("[Mailgun] Simulating API call to domain '{}' - API Key: {}...",
                config.getDomain(), maskApiKey(config.getApiKey()));
    }

    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
