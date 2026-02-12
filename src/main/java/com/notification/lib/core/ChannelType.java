package com.notification.lib.core;

/**
 * Represents the available notification channel types.
 *
 * <p><b>How to add a new notification channel:</b></p>
 * <ol>
 *   <li>Add a new enum value here (e.g., {@code WHATSAPP})</li>
 *   <li>Create a message class extending {@link ChannelMessage}
 *       (e.g., {@code WhatsAppMessage}) with channel-specific fields</li>
 *   <li>Create a provider interface (e.g., {@code WhatsAppProvider})
 *       with {@code send()} and {@code getName()} methods</li>
 *   <li>Create one or more provider implementations
 *       (e.g., {@code TwilioWhatsAppProvider})</li>
 *   <li>Create a channel class implementing {@link NotificationChannel}
 *       (e.g., {@code WhatsAppChannel}) that wires validation and provider</li>
 *   <li>Create a validator implementing
 *       {@link com.notification.lib.validation.MessageValidator}
 *       (e.g., {@code WhatsAppMessageValidator})</li>
 *   <li>Register the channel in configuration:
 *       {@code config.registerChannel(ChannelType.WHATSAPP, whatsAppChannel)}</li>
 * </ol>
 *
 * <p>The core {@link NotificationService} and {@link NotificationChannel} interface
 * require no modifications when adding a new channel - only this enum needs a new value.</p>
 */
public enum ChannelType {
    EMAIL,
    SMS,
    PUSH,
    SLACK
}
