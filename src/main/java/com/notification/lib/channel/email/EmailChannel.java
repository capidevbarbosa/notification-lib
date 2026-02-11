package com.notification.lib.channel.email;

import com.notification.lib.channel.email.provider.EmailProvider;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationChannel;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.ValidationException;
import com.notification.lib.validation.EmailMessageValidator;
import com.notification.lib.validation.MessageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Email notification channel implementation.
 * Delegates to a configured {@link EmailProvider} for actual delivery.
 *
 * <p>Follows Template Method pattern: validate -> send -> return result.
 * The provider is injected via constructor (Dependency Inversion).</p>
 */
public class EmailChannel implements NotificationChannel<EmailMessage> {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    private final EmailProvider provider;
    private final MessageValidator<EmailMessage> validator;

    /**
     * Creates an EmailChannel with the given provider and default validator.
     */
    public EmailChannel(EmailProvider provider) {
        this(provider, new EmailMessageValidator());
    }

    /**
     * Creates an EmailChannel with a custom validator (useful for testing or custom rules).
     */
    public EmailChannel(EmailProvider provider, MessageValidator<EmailMessage> validator) {
        if (provider == null) {
            throw new IllegalArgumentException("EmailProvider must not be null");
        }
        if (validator == null) {
            throw new IllegalArgumentException("MessageValidator must not be null");
        }
        this.provider = provider;
        this.validator = validator;
    }

    @Override
    public NotificationResult send(EmailMessage message) {
        log.debug("EmailChannel: Validating email message...");

        List<String> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            throw new ValidationException("Email validation failed", violations);
        }

        log.debug("EmailChannel: Validation passed. Delegating to provider '{}'", provider.getName());
        return provider.send(message);
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }

    @Override
    public String getProviderName() {
        return provider.getName();
    }
}
