package com.notification.lib.channel.push.provider;

import com.notification.lib.channel.push.PushMessage;
import com.notification.lib.core.NotificationResult;

/**
 * Abstraction for push notification providers.
 * Each provider (Firebase FCM, Apple APNs, OneSignal, etc.) implements this interface.
 */
public interface PushProvider {

    NotificationResult send(PushMessage message);

    String getName();
}
