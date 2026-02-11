package com.notification.lib.channel.sms;

import com.notification.lib.core.ChannelMessage;
import com.notification.lib.core.ChannelType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * SMS-specific message containing fields needed by SMS providers.
 * Modeled after Twilio's Messages API to ensure realistic design.
 *
 * <p>Fields like fromNumber and countryCode reflect what Twilio's
 * REST API expects when sending SMS messages.</p>
 */
@Getter
@SuperBuilder
public class SmsMessage extends ChannelMessage {

    /**
     * The sender phone number or alphanumeric ID (e.g., "+1234567890" or "MyApp").
     * Corresponds to Twilio's "From" field.
     */
    private final String fromNumber;

    /**
     * ISO 3166-1 alpha-2 country code for the recipient (e.g., "US", "CO").
     * Used for phone number validation and formatting.
     */
    private final String countryCode;

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SMS;
    }
}
