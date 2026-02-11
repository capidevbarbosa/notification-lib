package com.notification.lib.channel.email.provider;

import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.core.NotificationResult;

/**
 * Abstraction for email service providers.
 * Each provider (SendGrid, Mailgun, SES, etc.) implements this interface.
 *
 * <p><b>Dependency Inversion Principle:</b> EmailChannel depends on this
 * abstraction, not on concrete provider implementations.</p>
 */
public interface EmailProvider {

    /**
     * Sends an email message through this provider.
     *
     * @param message the email to send
     * @return the result of the operation
     */
    NotificationResult send(EmailMessage message);

    /**
     * Returns the provider name (e.g., "SendGrid", "Mailgun").
     */
    String getName();
}
