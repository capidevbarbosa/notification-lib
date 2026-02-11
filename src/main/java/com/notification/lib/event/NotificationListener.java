package com.notification.lib.event;

/**
 * Listener interface for notification lifecycle events.
 * Implements the Observer/Pub-Sub pattern to decouple
 * notification sending from status tracking/monitoring.
 *
 * <p>Consumers can implement this interface to:</p>
 * <ul>
 *   <li>Log notification events to a monitoring system</li>
 *   <li>Track delivery metrics</li>
 *   <li>Trigger follow-up actions on success/failure</li>
 *   <li>Update UI state</li>
 * </ul>
 */
@FunctionalInterface
public interface NotificationListener {

    /**
     * Called when a notification event occurs.
     *
     * @param event the notification event with status details
     */
    void onEvent(NotificationEvent event);
}
