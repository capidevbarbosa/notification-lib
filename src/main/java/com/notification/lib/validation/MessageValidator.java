package com.notification.lib.validation;

import com.notification.lib.core.ChannelMessage;

import java.util.List;

/**
 * Functional interface for validating channel-specific messages.
 * Each channel provides its own validator implementation.
 *
 * <p><b>Interface Segregation Principle:</b> Validators are separate from
 * channels, keeping validation logic decoupled from send logic.</p>
 *
 * @param <T> the specific message type to validate
 */
@FunctionalInterface
public interface MessageValidator<T extends ChannelMessage> {

    /**
     * Validates the given message and returns a list of violations.
     * An empty list means the message is valid.
     *
     * @param message the message to validate
     * @return list of validation error descriptions; empty if valid
     */
    List<String> validate(T message);
}
