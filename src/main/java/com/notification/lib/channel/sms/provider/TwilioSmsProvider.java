package com.notification.lib.channel.sms.provider;

import com.notification.lib.channel.sms.SmsMessage;
import com.notification.lib.config.ProviderConfig;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.SendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Simulated Twilio SMS provider.
 *
 * <p>In a real implementation, this would use Twilio's Messages API
 * (POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json) with:
 * - Basic Auth: {AccountSid}:{AuthToken}
 * - Form data: From, To, Body fields.
 * - Response includes: sid, status, date_created, etc.</p>
 *
 * <p>The config requires: apiKey (Account SID), apiSecret (Auth Token),
 * and fromNumber.</p>
 */
public class TwilioSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);
    private static final String PROVIDER_NAME = "Twilio";

    private final ProviderConfig config;

    public TwilioSmsProvider(ProviderConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ProviderConfig must not be null");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("API key (Account SID) is required for Twilio provider");
        }
        if (config.getApiSecret() == null || config.getApiSecret().isBlank()) {
            throw new IllegalArgumentException("API secret (Auth Token) is required for Twilio provider");
        }
        this.config = config;
    }

    @Override
    public NotificationResult send(SmsMessage message) {
        log.info("[Twilio] Sending SMS to '{}'", message.getRecipient());
        log.debug("[Twilio] From: {}, Body length: {}", message.getFromNumber(), message.getBody().length());

        try {
            simulateApiCall(message);

            // Twilio returns SIDs like "SMxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            String messageSid = "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
            log.info("[Twilio] SMS sent successfully. SID: {}", messageSid);

            return NotificationResult.success(messageSid, PROVIDER_NAME, ChannelType.SMS);

        } catch (Exception e) {
            log.error("[Twilio] Failed to send SMS to '{}': {}", message.getRecipient(), e.getMessage());
            throw new SendException("Twilio failed to send SMS: " + e.getMessage(), PROVIDER_NAME, e);
        }
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    private void simulateApiCall(SmsMessage message) {
        log.debug("[Twilio] Simulating API call - Account SID: {}..., To: {}, From: {}",
                maskApiKey(config.getApiKey()), message.getRecipient(), message.getFromNumber());
    }

    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
