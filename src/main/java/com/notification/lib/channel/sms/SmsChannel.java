package com.notification.lib.channel.sms;

import com.notification.lib.channel.sms.provider.SmsProvider;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationChannel;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.ValidationException;
import com.notification.lib.validation.MessageValidator;
import com.notification.lib.validation.SmsMessageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SMS notification channel implementation.
 * Delegates to a configured {@link SmsProvider} for actual delivery.
 */
public class SmsChannel implements NotificationChannel<SmsMessage> {

    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    private final SmsProvider provider;
    private final MessageValidator<SmsMessage> validator;

    public SmsChannel(SmsProvider provider) {
        this(provider, new SmsMessageValidator());
    }

    public SmsChannel(SmsProvider provider, MessageValidator<SmsMessage> validator) {
        if (provider == null) {
            throw new IllegalArgumentException("SmsProvider must not be null");
        }
        if (validator == null) {
            throw new IllegalArgumentException("MessageValidator must not be null");
        }
        this.provider = provider;
        this.validator = validator;
    }

    @Override
    public NotificationResult send(SmsMessage message) {
        log.debug("SmsChannel: Validating SMS message...");

        List<String> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            throw new ValidationException("SMS validation failed", violations);
        }

        log.debug("SmsChannel: Validation passed. Delegating to provider '{}'", provider.getName());
        return provider.send(message);
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SMS;
    }

    @Override
    public String getProviderName() {
        return provider.getName();
    }
}
