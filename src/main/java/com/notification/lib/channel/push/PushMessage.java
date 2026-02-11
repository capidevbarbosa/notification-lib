package com.notification.lib.channel.push;

import com.notification.lib.core.ChannelMessage;
import com.notification.lib.core.ChannelType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Push notification message containing fields needed by push providers.
 * Modeled after Firebase Cloud Messaging (FCM) HTTP v1 API to ensure
 * the design is realistic and could support actual integrations.
 *
 * <p>Fields like title, deviceToken, data, priority, and topic
 * reflect what FCM's API expects for push notifications.</p>
 */
@Getter
@SuperBuilder
public class PushMessage extends ChannelMessage {

    /**
     * The notification title displayed to the user.
     */
    private final String title;

    /**
     * The device registration token (FCM token).
     * Uniquely identifies the target device.
     */
    private final String deviceToken;

    /**
     * Custom key-value data payload sent with the notification.
     * The app can process this data in the background.
     */
    private final Map<String, String> data;

    /**
     * Notification priority: "high" or "normal".
     * High priority wakes the device; normal may be batched.
     */
    private final String priority;

    /**
     * Optional topic for topic-based messaging (e.g., "news", "offers").
     * If set, deviceToken can be null for topic-based delivery.
     */
    private final String topic;

    /**
     * Optional URL of an image to display in the notification.
     */
    private final String imageUrl;

    @Override
    public ChannelType getChannelType() {
        return ChannelType.PUSH;
    }
}
