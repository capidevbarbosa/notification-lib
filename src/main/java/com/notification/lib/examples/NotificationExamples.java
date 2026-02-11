package com.notification.lib.examples;

import com.notification.lib.async.AsyncNotificationService;
import com.notification.lib.channel.email.EmailChannel;
import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.channel.email.provider.SendGridProvider;
import com.notification.lib.channel.push.PushChannel;
import com.notification.lib.channel.push.PushMessage;
import com.notification.lib.channel.push.provider.FirebasePushProvider;
import com.notification.lib.channel.sms.SmsChannel;
import com.notification.lib.channel.sms.SmsMessage;
import com.notification.lib.channel.sms.provider.TwilioSmsProvider;
import com.notification.lib.config.NotificationConfig;
import com.notification.lib.config.ProviderConfig;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.core.NotificationService;
import com.notification.lib.event.NotificationStatus;
import com.notification.lib.exception.ValidationException;
import com.notification.lib.retry.RetryPolicy;
import com.notification.lib.template.MessageTemplate;
import com.notification.lib.template.TemplateEngine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstrates all features of the notification library.
 * Run this class to see the library in action with simulated providers.
 */
public class NotificationExamples {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Notification Library - Examples ===\n");

        example1_BasicEmailSend();
        example2_SmsSend();
        example3_PushNotification();
        example4_MultiChannelService();
        example5_RetryMechanism();
        example6_EventListeners();
        example7_AsyncSend();
        example8_Templates();
        example9_ErrorHandling();

        System.out.println("\n=== All examples completed successfully! ===");
    }

    /**
     * Example 1: Basic email sending with SendGrid provider.
     */
    static void example1_BasicEmailSend() {
        System.out.println("\n--- Example 1: Basic Email Send ---");

        // Configure the provider with API credentials
        ProviderConfig sendGridConfig = ProviderConfig.builder()
                .apiKey("SG.your-sendgrid-api-key-here")
                .build();

        // Create the email channel with the provider
        EmailChannel emailChannel = new EmailChannel(new SendGridProvider(sendGridConfig));

        // Build and send the email
        EmailMessage email = EmailMessage.builder()
                .recipient("user@example.com")
                .from("noreply@myapp.com")
                .subject("Welcome to MyApp!")
                .body("Hello! Thank you for signing up.")
                .htmlBody("<h1>Welcome!</h1><p>Thank you for signing up.</p>")
                .cc(List.of("admin@myapp.com"))
                .replyTo("support@myapp.com")
                .build();

        NotificationResult result = emailChannel.send(email);

        System.out.println("  Success: " + result.isSuccess());
        System.out.println("  Message ID: " + result.getMessageId());
        System.out.println("  Provider: " + result.getProviderName());
    }

    /**
     * Example 2: SMS sending with Twilio provider.
     */
    static void example2_SmsSend() {
        System.out.println("\n--- Example 2: SMS Send ---");

        ProviderConfig twilioConfig = ProviderConfig.builder()
                .apiKey("AC-your-twilio-account-sid")
                .apiSecret("your-twilio-auth-token")
                .build();

        SmsChannel smsChannel = new SmsChannel(new TwilioSmsProvider(twilioConfig));

        SmsMessage sms = SmsMessage.builder()
                .recipient("+1234567890")
                .fromNumber("+0987654321")
                .body("Your verification code is: 123456")
                .countryCode("US")
                .build();

        NotificationResult result = smsChannel.send(sms);

        System.out.println("  Success: " + result.isSuccess());
        System.out.println("  Message SID: " + result.getMessageId());
    }

    /**
     * Example 3: Push notification with Firebase provider.
     */
    static void example3_PushNotification() {
        System.out.println("\n--- Example 3: Push Notification ---");

        ProviderConfig firebaseConfig = ProviderConfig.builder()
                .apiKey("your-firebase-server-key")
                .properties(Map.of("projectId", "my-app-project"))
                .build();

        PushChannel pushChannel = new PushChannel(new FirebasePushProvider(firebaseConfig));

        PushMessage push = PushMessage.builder()
                .deviceToken("fcm-device-token-abc123xyz")
                .title("New Order!")
                .body("Your order #1234 has been shipped.")
                .data(Map.of("orderId", "1234", "action", "VIEW_ORDER"))
                .priority("high")
                .build();

        NotificationResult result = pushChannel.send(push);

        System.out.println("  Success: " + result.isSuccess());
        System.out.println("  FCM Message: " + result.getMessageId());
    }

    /**
     * Example 4: Using NotificationService with multiple channels.
     */
    static void example4_MultiChannelService() {
        System.out.println("\n--- Example 4: Multi-Channel Service ---");

        // Configure all channels
        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, new EmailChannel(
                        new SendGridProvider(ProviderConfig.withApiKey("SG.api-key"))))
                .registerChannel(ChannelType.SMS, new SmsChannel(
                        new TwilioSmsProvider(ProviderConfig.builder()
                                .apiKey("AC-sid").apiSecret("auth-token").build())))
                .registerChannel(ChannelType.PUSH, new PushChannel(
                        new FirebasePushProvider(ProviderConfig.withApiKey("firebase-key"))))
                .build();

        NotificationService service = new NotificationService(config);

        // Send email through the service
        NotificationResult emailResult = service.send(EmailMessage.builder()
                .recipient("user@example.com")
                .from("noreply@app.com")
                .subject("Order Confirmed")
                .body("Your order has been confirmed!")
                .build());

        // Send SMS through the same service
        NotificationResult smsResult = service.send(SmsMessage.builder()
                .recipient("+1234567890")
                .fromNumber("+0987654321")
                .body("Order confirmed! Track at: https://app.com/track/1234")
                .build());

        System.out.println("  Email sent: " + emailResult.isSuccess());
        System.out.println("  SMS sent: " + smsResult.isSuccess());
    }

    /**
     * Example 5: Retry mechanism with exponential backoff.
     */
    static void example5_RetryMechanism() {
        System.out.println("\n--- Example 5: Retry Mechanism ---");

        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, new EmailChannel(
                        new SendGridProvider(ProviderConfig.withApiKey("SG.api-key"))))
                .withRetryPolicy(RetryPolicy.builder()
                        .maxRetries(3)
                        .initialDelayMs(100)
                        .multiplier(2.0)
                        .maxDelayMs(5000)
                        .build())
                .build();

        NotificationService service = new NotificationService(config);

        NotificationResult result = service.send(EmailMessage.builder()
                .recipient("user@example.com")
                .from("noreply@app.com")
                .subject("With Retry")
                .body("This will retry on failure")
                .build());

        System.out.println("  Result: " + result.isSuccess());
    }

    /**
     * Example 6: Event listeners for notification lifecycle tracking.
     */
    static void example6_EventListeners() {
        System.out.println("\n--- Example 6: Event Listeners (Pub/Sub) ---");

        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, new EmailChannel(
                        new SendGridProvider(ProviderConfig.withApiKey("SG.api-key"))))
                .addListener(event -> {
                    System.out.println("  [EVENT] " + event.getStatus() + " - "
                            + event.getChannelType() + " - " + event.getMessageId());
                    if (event.getStatus() == NotificationStatus.SENT) {
                        System.out.println("  [EVENT] Delivered via: " + event.getResult().getProviderName());
                    }
                })
                .build();

        NotificationService service = new NotificationService(config);

        service.send(EmailMessage.builder()
                .recipient("user@example.com")
                .from("noreply@app.com")
                .subject("With Events")
                .body("Lifecycle events will be printed")
                .build());
    }

    /**
     * Example 7: Async notification sending.
     */
    static void example7_AsyncSend() throws Exception {
        System.out.println("\n--- Example 7: Async Send ---");

        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, new EmailChannel(
                        new SendGridProvider(ProviderConfig.withApiKey("SG.api-key"))))
                .build();

        NotificationService service = new NotificationService(config);
        AsyncNotificationService asyncService = new AsyncNotificationService(service);

        // Single async send
        CompletableFuture<NotificationResult> future = asyncService.sendAsync(
                EmailMessage.builder()
                        .recipient("user@example.com")
                        .from("noreply@app.com")
                        .subject("Async Email")
                        .body("Sent asynchronously!")
                        .build());

        NotificationResult result = future.get();
        System.out.println("  Async result: " + result.isSuccess());

        // Batch send
        List<EmailMessage> emails = List.of(
                EmailMessage.builder().recipient("user1@example.com").from("noreply@app.com")
                        .subject("Batch 1").body("Message 1").build(),
                EmailMessage.builder().recipient("user2@example.com").from("noreply@app.com")
                        .subject("Batch 2").body("Message 2").build(),
                EmailMessage.builder().recipient("user3@example.com").from("noreply@app.com")
                        .subject("Batch 3").body("Message 3").build()
        );

        List<NotificationResult> batchResults = asyncService.sendBatch(emails).get();
        System.out.println("  Batch results: " + batchResults.size() + " sent");
    }

    /**
     * Example 8: Message templates.
     */
    static void example8_Templates() {
        System.out.println("\n--- Example 8: Message Templates ---");

        TemplateEngine templateEngine = new TemplateEngine();

        // Register templates
        templateEngine.register("welcome_email",
                new MessageTemplate("welcome_email",
                        "Hello {{name}}, welcome to {{appName}}! Your account has been created."));

        templateEngine.register("order_confirmation",
                new MessageTemplate("order_confirmation",
                        "Hi {{name}}, your order #{{orderId}} for {{amount}} has been confirmed."));

        // Render templates with variables
        String welcomeBody = templateEngine.render("welcome_email",
                Map.of("name", "Alice", "appName", "NotifyLib"));

        String orderBody = templateEngine.render("order_confirmation",
                Map.of("name", "Bob", "orderId", "ORD-5678", "amount", "$99.99"));

        System.out.println("  Welcome: " + welcomeBody);
        System.out.println("  Order: " + orderBody);

        // Use rendered template in a notification
        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, new EmailChannel(
                        new SendGridProvider(ProviderConfig.withApiKey("SG.api-key"))))
                .build();
        NotificationService service = new NotificationService(config);

        service.send(EmailMessage.builder()
                .recipient("alice@example.com")
                .from("noreply@app.com")
                .subject("Welcome!")
                .body(welcomeBody)
                .build());

        System.out.println("  Templated email sent successfully!");
    }

    /**
     * Example 9: Error handling.
     */
    static void example9_ErrorHandling() {
        System.out.println("\n--- Example 9: Error Handling ---");

        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, new EmailChannel(
                        new SendGridProvider(ProviderConfig.withApiKey("SG.api-key"))))
                .build();

        NotificationService service = new NotificationService(config);

        // Validation error: invalid email format
        try {
            service.send(EmailMessage.builder()
                    .recipient("not-a-valid-email")
                    .from("noreply@app.com")
                    .subject("Test")
                    .body("Body")
                    .build());
        } catch (ValidationException e) {
            System.out.println("  Caught ValidationException: " + e.getMessage());
            System.out.println("  Violations: " + e.getViolations());
        }

        // Validation error: missing required fields
        try {
            service.send(EmailMessage.builder().build());
        } catch (ValidationException e) {
            System.out.println("  Caught ValidationException (missing fields): " + e.getMessage());
        }

        System.out.println("  Error handling works correctly!");
    }
}
