package com.notification.lib.channel.email;

import com.notification.lib.core.ChannelMessage;
import com.notification.lib.core.ChannelType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Email-specific message containing all fields needed by email providers.
 * Modeled after real provider APIs (SendGrid, Mailgun) to ensure
 * the design is realistic and could support actual integrations.
 *
 * <p>Fields like cc, bcc, replyTo, and htmlBody reflect what providers
 * like SendGrid's v3 API and Mailgun's Messages API expect.</p>
 */
@Getter
@SuperBuilder
public class EmailMessage extends ChannelMessage {

    /**
     * Sender email address (e.g., "noreply@company.com").
     */
    private final String from;

    /**
     * Email subject line.
     */
    private final String subject;

    /**
     * HTML body content. If provided, takes precedence over plain text body.
     */
    private final String htmlBody;

    /**
     * Carbon copy recipients.
     */
    private final List<String> cc;

    /**
     * Blind carbon copy recipients.
     */
    private final List<String> bcc;

    /**
     * Reply-to email address.
     */
    private final String replyTo;

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }
}
