package com.notification.lib.validation;

import com.notification.lib.channel.push.PushMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates push notification messages before sending.
 * Checks for required fields based on FCM API requirements.
 */
public class PushMessageValidator implements MessageValidator<PushMessage> {

    @Override
    public List<String> validate(PushMessage message) {
        List<String> violations = new ArrayList<>();

        if (message == null) {
            violations.add("Push message must not be null");
            return violations;
        }

        // Either deviceToken or topic must be provided (FCM supports both delivery methods)
        boolean hasDeviceToken = !isBlank(message.getDeviceToken());
        boolean hasTopic = !isBlank(message.getTopic());

        if (!hasDeviceToken && !hasTopic) {
            violations.add("Either deviceToken or topic must be provided");
        }

        if (isBlank(message.getTitle())) {
            violations.add("Push notification title is required");
        }

        if (isBlank(message.getBody())) {
            violations.add("Push notification body is required");
        }

        // Validate priority if provided
        if (message.getPriority() != null
                && !message.getPriority().equals("high")
                && !message.getPriority().equals("normal")) {
            violations.add("Priority must be 'high' or 'normal', got: " + message.getPriority());
        }

        return violations;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
