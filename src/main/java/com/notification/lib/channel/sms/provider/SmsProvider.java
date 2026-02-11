package com.notification.lib.channel.sms.provider;

import com.notification.lib.channel.sms.SmsMessage;
import com.notification.lib.core.NotificationResult;

/**
 * Abstraction for SMS service providers.
 * Each provider (Twilio, Vonage, AWS SNS, etc.) implements this interface.
 */
public interface SmsProvider {

    NotificationResult send(SmsMessage message);

    String getName();
}
