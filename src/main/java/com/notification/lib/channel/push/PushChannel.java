package com.notification.lib.channel.push;

import com.notification.lib.channel.push.provider.PushProvider;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationChannel;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.ValidationException;
import com.notification.lib.validation.MessageValidator;
import com.notification.lib.validation.PushMessageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Push notification channel implementation.
 * Delegates to a configured {@link PushProvider} for actual delivery.
 */
public class PushChannel implements NotificationChannel<PushMessage> {

    private static final Logger log = LoggerFactory.getLogger(PushChannel.class);

    private final PushProvider provider;
    private final MessageValidator<PushMessage> validator;

    public PushChannel(PushProvider provider) {
        this(provider, new PushMessageValidator());
    }

    public PushChannel(PushProvider provider, MessageValidator<PushMessage> validator) {
        if (provider == null) {
            throw new IllegalArgumentException("PushProvider must not be null");
        }
        if (validator == null) {
            throw new IllegalArgumentException("MessageValidator must not be null");
        }
        this.provider = provider;
        this.validator = validator;
    }

    @Override
    public NotificationResult send(PushMessage message) {
        log.debug("PushChannel: Validating push message...");

        List<String> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            throw new ValidationException("Push notification validation failed", violations);
        }

        log.debug("PushChannel: Validation passed. Delegating to provider '{}'", provider.getName());
        return provider.send(message);
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.PUSH;
    }

    @Override
    public String getProviderName() {
        return provider.getName();
    }
}
