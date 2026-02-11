package com.notification.lib.validation;

import com.notification.lib.channel.sms.SmsMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates SMS messages before sending.
 * Checks for required fields and valid phone number format.
 */
public class SmsMessageValidator implements MessageValidator<SmsMessage> {

    /**
     * E.164 phone number format: +[country code][number], 7-15 digits total.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+[1-9]\\d{6,14}$"
    );

    private static final int MAX_SMS_LENGTH = 1600;

    @Override
    public List<String> validate(SmsMessage message) {
        List<String> violations = new ArrayList<>();

        if (message == null) {
            violations.add("SMS message must not be null");
            return violations;
        }

        if (isBlank(message.getRecipient())) {
            violations.add("Recipient phone number is required");
        } else if (!isValidPhoneNumber(message.getRecipient())) {
            violations.add("Recipient phone number must be in E.164 format (e.g., +1234567890): " + message.getRecipient());
        }

        if (isBlank(message.getBody())) {
            violations.add("SMS body is required");
        } else if (message.getBody().length() > MAX_SMS_LENGTH) {
            violations.add("SMS body exceeds maximum length of " + MAX_SMS_LENGTH + " characters");
        }

        if (isBlank(message.getFromNumber())) {
            violations.add("Sender phone number (fromNumber) is required");
        }

        return violations;
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
