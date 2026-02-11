package com.notification.lib.validation;

import com.notification.lib.channel.email.EmailMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates email messages before sending.
 * Checks for required fields and valid email format.
 */
public class EmailMessageValidator implements MessageValidator<EmailMessage> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    @Override
    public List<String> validate(EmailMessage message) {
        List<String> violations = new ArrayList<>();

        if (message == null) {
            violations.add("Email message must not be null");
            return violations;
        }

        if (isBlank(message.getRecipient())) {
            violations.add("Recipient (to) is required");
        } else if (!isValidEmail(message.getRecipient())) {
            violations.add("Recipient email format is invalid: " + message.getRecipient());
        }

        if (isBlank(message.getFrom())) {
            violations.add("Sender (from) is required");
        } else if (!isValidEmail(message.getFrom())) {
            violations.add("Sender email format is invalid: " + message.getFrom());
        }

        if (isBlank(message.getSubject())) {
            violations.add("Subject is required");
        }

        if (isBlank(message.getBody()) && isBlank(message.getHtmlBody())) {
            violations.add("Either body or htmlBody must be provided");
        }

        // Validate CC emails if provided
        if (message.getCc() != null) {
            for (String cc : message.getCc()) {
                if (!isValidEmail(cc)) {
                    violations.add("Invalid CC email: " + cc);
                }
            }
        }

        // Validate BCC emails if provided
        if (message.getBcc() != null) {
            for (String bcc : message.getBcc()) {
                if (!isValidEmail(bcc)) {
                    violations.add("Invalid BCC email: " + bcc);
                }
            }
        }

        // Validate replyTo if provided
        if (message.getReplyTo() != null && !isValidEmail(message.getReplyTo())) {
            violations.add("Invalid replyTo email: " + message.getReplyTo());
        }

        return violations;
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
