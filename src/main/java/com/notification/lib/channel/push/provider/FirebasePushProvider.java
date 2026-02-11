package com.notification.lib.channel.push.provider;

import com.notification.lib.channel.push.PushMessage;
import com.notification.lib.config.ProviderConfig;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.SendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Simulated Firebase Cloud Messaging (FCM) push provider.
 *
 * <p>In a real implementation, this would use FCM HTTP v1 API
 * (POST https://fcm.googleapis.com/v1/projects/{project_id}/messages:send) with:
 * - Authorization: Bearer {access_token} (OAuth 2.0)
 * - Content-Type: application/json
 * - Request body with message.token, message.notification (title, body, image),
 *   message.data, and message.android/apns/webpush platform-specific config.</p>
 *
 * <p>The config requires: apiKey (server key or service account), projectId.</p>
 */
public class FirebasePushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushProvider.class);
    private static final String PROVIDER_NAME = "Firebase";

    private final ProviderConfig config;

    public FirebasePushProvider(ProviderConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ProviderConfig must not be null");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("API key (server key) is required for Firebase provider");
        }
        this.config = config;
    }

    @Override
    public NotificationResult send(PushMessage message) {
        log.info("[Firebase] Sending push notification to device '{}'",
                message.getDeviceToken() != null ? maskToken(message.getDeviceToken()) : "topic:" + message.getTopic());
        log.debug("[Firebase] Title: '{}', Priority: {}, Data keys: {}",
                message.getTitle(),
                message.getPriority(),
                message.getData() != null ? message.getData().keySet() : "none");

        try {
            simulateApiCall(message);

            // FCM returns message names like "projects/{project}/messages/{id}"
            String messageId = "projects/" + getProjectId() + "/messages/" + UUID.randomUUID().toString().substring(0, 12);
            log.info("[Firebase] Push notification sent successfully. Message: {}", messageId);

            return NotificationResult.success(messageId, PROVIDER_NAME, ChannelType.PUSH);

        } catch (Exception e) {
            log.error("[Firebase] Failed to send push notification: {}", e.getMessage());
            throw new SendException("Firebase failed to send push notification: " + e.getMessage(), PROVIDER_NAME, e);
        }
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    private void simulateApiCall(PushMessage message) {
        log.debug("[Firebase] Simulating FCM API call - Project: {}", getProjectId());
    }

    private String getProjectId() {
        String projectId = config.getProperties().get("projectId");
        return projectId != null ? projectId : "default-project";
    }

    private String maskToken(String token) {
        if (token.length() <= 10) return "****";
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
