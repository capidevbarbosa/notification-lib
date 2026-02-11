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
 * Simulated SendGrid email provider.
 *
 * <p>In a real implementation, this would use SendGrid's v3 Mail Send API
 * (POST https://api.sendgrid.com/v3/mail/send) with:
 * - Authorization: Bearer {API_KEY}
 * - Content-Type: application/json
 * - Request body containing personalizations, from, subject, and content arrays.</p>
 *
 * <p>The simulation logs the operation and returns a realistic response
 * structure matching what SendGrid would return (202 Accepted with X-Message-Id).</p>
 */
public class SendGridProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SendGridProvider.class);
    private static final String PROVIDER_NAME = "SendGrid";

    private final ProviderConfig config;

    public SendGridProvider(ProviderConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ProviderConfig must not be null");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("API key is required for SendGrid provider");
        }
        this.config = config;
    }

    @Override
    public NotificationResult send(EmailMessage message) {
        log.info("[SendGrid] Sending email to '{}' with subject '{}'", message.getRecipient(), message.getSubject());
        log.debug("[SendGrid] From: {}, CC: {}, BCC: {}", message.getFrom(), message.getCc(), message.getBcc());

        try {
            // Simulate API call to SendGrid
            // POST https://api.sendgrid.com/v3/mail/send
            simulateApiCall(message);

            String messageId = "sg-" + UUID.randomUUID().toString().substring(0, 12);
            log.info("[SendGrid] Email sent successfully. Message ID: {}", messageId);

            return NotificationResult.success(messageId, PROVIDER_NAME, ChannelType.EMAIL);

        } catch (Exception e) {
            log.error("[SendGrid] Failed to send email to '{}': {}", message.getRecipient(), e.getMessage());
            throw new SendException("SendGrid failed to send email: " + e.getMessage(), PROVIDER_NAME, e);
        }
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    /**
     * Simulates the HTTP call to SendGrid API.
     * In production, this would make a real HTTP POST request.
     */
    private void simulateApiCall(EmailMessage message) {
        log.debug("[SendGrid] Simulating API call - API Key: {}...", maskApiKey(config.getApiKey()));
        log.debug("[SendGrid] Payload: to={}, from={}, subject={}, bodyLength={}",
                message.getRecipient(),
                message.getFrom(),
                message.getSubject(),
                message.getBody() != null ? message.getBody().length() : 0);
    }

    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
