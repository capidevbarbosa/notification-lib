package com.notification.lib.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages notification event listeners and publishes events to them.
 * Thread-safe implementation using CopyOnWriteArrayList.
 *
 * <p>Listeners are invoked synchronously. Exceptions in listeners
 * are caught and logged to prevent one faulty listener from
 * blocking the notification flow.</p>
 */
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final CopyOnWriteArrayList<NotificationListener> listeners;

    public NotificationEventPublisher() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public NotificationEventPublisher(List<NotificationListener> initialListeners) {
        this.listeners = new CopyOnWriteArrayList<>(initialListeners);
    }

    /**
     * Adds a listener to receive notification events.
     */
    public void addListener(NotificationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     */
    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Publishes an event to all registered listeners.
     * Each listener is invoked in a try-catch to ensure fault isolation.
     */
    public void publish(NotificationEvent event) {
        for (NotificationListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.warn("Notification listener threw an exception: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the number of registered listeners.
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
