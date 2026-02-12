package com.notification.lib.async;

import com.notification.lib.core.ChannelMessage;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.core.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Asynchronous wrapper around {@link NotificationService}.
 * Provides non-blocking notification sending using {@link CompletableFuture}.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>Single async sends with CompletableFuture</li>
 *   <li>Batch sending of multiple notifications</li>
 *   <li>Custom executor for thread pool control</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>
 * AsyncNotificationService asyncService = new AsyncNotificationService(service);
 *
 * asyncService.sendAsync(emailMessage)
 *     .thenAccept(result -> System.out.println("Sent: " + result.getMessageId()))
 *     .exceptionally(ex -> { System.err.println("Failed: " + ex.getMessage()); return null; });
 * </pre>
 */
public class AsyncNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationService.class);

    private final NotificationService notificationService;
    private final Executor executor;

    /**
     * Creates an AsyncNotificationService with a default cached thread pool executor.
     *
     * <p>Note: For Java 21+, consider passing {@code Executors.newVirtualThreadPerTaskExecutor()}
     * as a custom executor for lightweight concurrency with virtual threads.</p>
     */
    public AsyncNotificationService(NotificationService notificationService) {
        this(notificationService, Executors.newCachedThreadPool());
    }

    /**
     * Creates an AsyncNotificationService with a custom executor.
     *
     * @param notificationService the underlying sync service
     * @param executor the executor for async operations
     */
    public AsyncNotificationService(NotificationService notificationService, Executor executor) {
        if (notificationService == null) {
            throw new IllegalArgumentException("NotificationService must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null");
        }
        this.notificationService = notificationService;
        this.executor = executor;
    }

    /**
     * Sends a notification asynchronously.
     *
     * @param message the message to send
     * @return a CompletableFuture that completes with the result
     */
    public <T extends ChannelMessage> CompletableFuture<NotificationResult> sendAsync(T message) {
        log.debug("Scheduling async send for {} message [{}]", message.getChannelType(), message.getId());

        return CompletableFuture.supplyAsync(
                () -> notificationService.send(message),
                executor
        );
    }

    /**
     * Sends multiple notifications in batch asynchronously.
     * All notifications are submitted concurrently and results are collected.
     *
     * @param messages the list of messages to send
     * @return a CompletableFuture that completes with all results
     */
    public CompletableFuture<List<NotificationResult>> sendBatch(List<? extends ChannelMessage> messages) {
        log.info("Sending batch of {} notifications", messages.size());

        List<CompletableFuture<NotificationResult>> futures = messages.stream()
                .map(this::sendAsync)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    /**
     * Sends multiple notifications in batch, collecting results even if some fail.
     * Unlike {@link #sendBatch}, this method does not fail fast on individual errors.
     *
     * @param messages the list of messages to send
     * @return a CompletableFuture with results for all messages (success and failure)
     */
    public CompletableFuture<List<NotificationResult>> sendBatchSettled(List<? extends ChannelMessage> messages) {
        log.info("Sending batch (settled) of {} notifications", messages.size());

        List<CompletableFuture<NotificationResult>> futures = messages.stream()
                .map(msg -> sendAsync(msg)
                        .exceptionally(ex -> {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            log.warn("Batch item failed: {}", cause.getMessage());
                            return NotificationResult.failure("unknown", msg.getChannelType(),
                                    cause.getClass().getSimpleName(), cause.getMessage());
                        }))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
}
