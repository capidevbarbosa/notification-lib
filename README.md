# Notification Library

A **framework-agnostic** Java notification library that provides a unified API for sending notifications across multiple channels (Email, SMS, Push Notifications). Designed with extensibility in mind - swap providers or add new channels without modifying existing code.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Channels & Providers](#channels--providers)
- [Features](#features)
- [API Reference](#api-reference)
- [Extending the Library](#extending-the-library)
- [Security Best Practices](#security-best-practices)
- [Running with Docker](#running-with-docker)
- [Testing](#testing)
- [Design Decisions](#design-decisions)

---

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.notification.lib:notification-lib:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.notification.lib:notification-lib:1.0.0'
}
```

### Build from source

```bash
git clone <repository-url>
cd notification-lib
./gradlew build
```

The JAR will be generated at `build/libs/notification-lib-1.0.0.jar`.

---

## Quick Start

```java
import com.notification.lib.channel.email.*;
import com.notification.lib.channel.email.provider.*;
import com.notification.lib.config.*;
import com.notification.lib.core.*;

// 1. Configure the provider
ProviderConfig sendGridConfig = ProviderConfig.builder()
    .apiKey(System.getenv("SENDGRID_API_KEY"))
    .build();

// 2. Create the channel with the provider
EmailChannel emailChannel = new EmailChannel(new SendGridProvider(sendGridConfig));

// 3. Configure the service
NotificationConfig config = NotificationConfig.builder()
    .registerChannel(ChannelType.EMAIL, emailChannel)
    .build();

NotificationService service = new NotificationService(config);

// 4. Send a notification
NotificationResult result = service.send(EmailMessage.builder()
    .recipient("user@example.com")
    .from("noreply@myapp.com")
    .subject("Welcome!")
    .body("Hello, welcome to our platform!")
    .build());

if (result.isSuccess()) {
    System.out.println("Sent! ID: " + result.getMessageId());
}
```

---

## Architecture

### Design Patterns

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Strategy** | `NotificationChannel<T>` | Each channel is an interchangeable sending strategy |
| **Builder** | Messages, Config, Results | Fluent and type-safe object construction |
| **Facade** | `NotificationService` | Single entry point for the notification subsystem |
| **Observer** | `NotificationEventPublisher` | Decouple sending from status tracking |
| **Template Method** | Channel implementations | Validate -> Send -> Return result flow |

### SOLID Principles

- **S** - Single Responsibility: Each class has one reason to change (`EmailChannel` sends, `EmailMessageValidator` validates)
- **O** - Open/Closed: Add new channels by implementing `NotificationChannel<T>` without modifying existing code
- **L** - Liskov Substitution: All channel implementations are interchangeable where `NotificationChannel<T>` is expected
- **I** - Interface Segregation: Small, focused interfaces (`MessageValidator`, `NotificationListener`, `EmailProvider`)
- **D** - Dependency Inversion: `NotificationService` depends on abstractions (`NotificationChannel`), not concrete providers

### Package Structure

```
com.notification.lib
├── core/           # Core interfaces and models
├── channel/        # Channel implementations
│   ├── email/      #   Email channel + providers
│   ├── sms/        #   SMS channel + providers
│   └── push/       #   Push notification channel + providers
├── config/         # Configuration builders
├── exception/      # Exception hierarchy
├── validation/     # Message validators
├── retry/          # Retry mechanism with exponential backoff
├── async/          # Async notification support
├── template/       # Message template engine
├── event/          # Event publishing (Pub/Sub)
└── examples/       # Usage examples
```

---

## Configuration

All configuration is done through Java code. No YAML, properties, or annotation-based configuration required.

### Multi-channel setup

```java
NotificationConfig config = NotificationConfig.builder()
    // Email via SendGrid
    .registerChannel(ChannelType.EMAIL, new EmailChannel(
        new SendGridProvider(ProviderConfig.builder()
            .apiKey(System.getenv("SENDGRID_API_KEY"))
            .build())))
    
    // SMS via Twilio
    .registerChannel(ChannelType.SMS, new SmsChannel(
        new TwilioSmsProvider(ProviderConfig.builder()
            .apiKey(System.getenv("TWILIO_ACCOUNT_SID"))
            .apiSecret(System.getenv("TWILIO_AUTH_TOKEN"))
            .build())))
    
    // Push via Firebase
    .registerChannel(ChannelType.PUSH, new PushChannel(
        new FirebasePushProvider(ProviderConfig.builder()
            .apiKey(System.getenv("FIREBASE_SERVER_KEY"))
            .properties(Map.of("projectId", "my-project"))
            .build())))
    
    // Optional: retry policy
    .withRetryPolicy(RetryPolicy.builder()
        .maxRetries(3)
        .initialDelayMs(1000)
        .multiplier(2.0)
        .build())
    
    // Optional: event listeners
    .addListener(event -> log.info("Notification {}: {}", event.getMessageId(), event.getStatus()))
    
    .build();

NotificationService service = new NotificationService(config);
```

### Switching providers

Changing from SendGrid to Mailgun requires zero changes to your sending code:

```java
// Before: SendGrid
EmailChannel emailChannel = new EmailChannel(
    new SendGridProvider(ProviderConfig.withApiKey("SG.xxx")));

// After: Mailgun - only the provider changes
EmailChannel emailChannel = new EmailChannel(
    new MailgunProvider(ProviderConfig.builder()
        .apiKey("key-xxx")
        .domain("mg.example.com")
        .build()));
```

---

## Channels & Providers

### Email

**Providers:** SendGrid, Mailgun

```java
EmailMessage email = EmailMessage.builder()
    .recipient("user@example.com")
    .from("noreply@app.com")
    .subject("Order Confirmed")
    .body("Your order #1234 is confirmed!")
    .htmlBody("<h1>Order Confirmed</h1><p>Your order #1234 is confirmed!</p>")
    .cc(List.of("manager@app.com"))
    .bcc(List.of("audit@app.com"))
    .replyTo("support@app.com")
    .metadata(Map.of("orderId", "1234"))
    .build();
```

### SMS

**Providers:** Twilio

```java
SmsMessage sms = SmsMessage.builder()
    .recipient("+1234567890")     // E.164 format required
    .fromNumber("+0987654321")
    .body("Your verification code is: 123456")
    .countryCode("US")
    .build();
```

### Push Notification

**Providers:** Firebase Cloud Messaging (FCM)

```java
PushMessage push = PushMessage.builder()
    .deviceToken("fcm-token-xxx")     // or .topic("news")
    .title("New Message")
    .body("You have a new message from Alice")
    .data(Map.of("chatId", "abc", "action", "OPEN_CHAT"))
    .priority("high")                  // "high" or "normal"
    .imageUrl("https://example.com/image.png")
    .build();
```

---

## Features

### Async Notifications

```java
AsyncNotificationService asyncService = new AsyncNotificationService(service);

// Single async send
CompletableFuture<NotificationResult> future = asyncService.sendAsync(emailMessage);
future.thenAccept(result -> System.out.println("Sent: " + result.getMessageId()))
      .exceptionally(ex -> { log.error("Failed", ex); return null; });

// Batch send (all concurrent)
List<NotificationResult> results = asyncService.sendBatch(messages).get();

// Batch with error tolerance (doesn't fail-fast)
List<NotificationResult> results = asyncService.sendBatchSettled(messages).get();
```

### Retry Mechanism

Exponential backoff with configurable parameters:

```java
RetryPolicy policy = RetryPolicy.builder()
    .maxRetries(3)          // Max 3 retry attempts
    .initialDelayMs(1000)   // First retry after 1s
    .multiplier(2.0)        // Double the delay each time
    .maxDelayMs(30000)      // Cap at 30s
    .build();
// Retry sequence: 1s -> 2s -> 4s (then give up)

// Attach to config
NotificationConfig config = NotificationConfig.builder()
    .registerChannel(ChannelType.EMAIL, emailChannel)
    .withRetryPolicy(policy)
    .build();
```

Only `SendException` triggers retries. `ValidationException` fails immediately (bad input won't improve with retries).

### Message Templates

```java
TemplateEngine engine = new TemplateEngine();

engine.register("welcome", new MessageTemplate("welcome",
    "Hello {{name}}, welcome to {{appName}}! Your account is ready."));

engine.register("order", new MessageTemplate("order",
    "Hi {{name}}, your order #{{orderId}} for {{amount}} has shipped."));

// Render with variables
String body = engine.render("welcome", Map.of(
    "name", "Alice",
    "appName", "MyApp"
));
// Result: "Hello Alice, welcome to MyApp! Your account is ready."

// Safe render (missing vars become empty string)
String safe = new MessageTemplate("Hi {{name}}!").renderSafe(Map.of());
// Result: "Hi !"
```

### Event Listeners (Pub/Sub)

Track the lifecycle of every notification:

```java
NotificationConfig config = NotificationConfig.builder()
    .registerChannel(ChannelType.EMAIL, emailChannel)
    .addListener(event -> {
        switch (event.getStatus()) {
            case PENDING  -> log.info("Queued: {}", event.getMessageId());
            case SENDING  -> log.info("Sending via {}", event.getChannelType());
            case SENT     -> metrics.increment("notifications.sent");
            case FAILED   -> alerting.notify("Send failed: " + event.getErrorMessage());
            case RETRYING -> log.warn("Retry attempt {}", event.getRetryAttempt());
        }
    })
    .build();
```

Events are published synchronously. Listener exceptions are caught and logged to prevent blocking the notification flow.

### Validation

Every channel validates messages before sending:

| Channel | Validations |
|---------|------------|
| Email | Valid email format (to, from, cc, bcc, replyTo), required subject, body or htmlBody |
| SMS | E.164 phone format, required body, max 1600 chars, required fromNumber |
| Push | Required deviceToken or topic, required title and body, priority must be "high" or "normal" |

---

## API Reference

### Core Classes

| Class | Description |
|-------|------------|
| `NotificationService` | Main facade - routes messages to channels |
| `NotificationChannel<T>` | Interface for channel implementations |
| `ChannelMessage` | Base class for channel-specific messages |
| `NotificationResult` | Send operation result (success/failure) |
| `NotificationConfig` | Builder-based configuration |
| `ProviderConfig` | Provider credentials and settings |

### Exceptions

| Exception | When |
|-----------|------|
| `NotificationException` | Base exception for all errors |
| `ValidationException` | Message fails validation (bad input) |
| `SendException` | Provider/transport failure |

### Optional Features

| Class | Description |
|-------|------------|
| `AsyncNotificationService` | Async wrapper with CompletableFuture |
| `RetryPolicy` | Configurable retry with exponential backoff |
| `RetryExecutor` | Executes operations with retry logic |
| `TemplateEngine` | Named template registry |
| `MessageTemplate` | Template with `{{variable}}` substitution |
| `NotificationEventPublisher` | Pub/Sub event manager |
| `NotificationListener` | Event listener interface |

---

## Extending the Library

### Adding a new channel (e.g., Slack)

1. **Create the message class:**

```java
@Getter
@SuperBuilder
public class SlackMessage extends ChannelMessage {
    private final String channel;      // #general
    private final String webhookUrl;
    
    @Override
    public ChannelType getChannelType() {
        return ChannelType.SLACK;
    }
}
```

2. **Create the provider interface and implementation:**

```java
public interface SlackProvider {
    NotificationResult send(SlackMessage message);
    String getName();
}

public class SlackWebhookProvider implements SlackProvider {
    // Implement webhook-based sending
}
```

3. **Create the channel:**

```java
public class SlackChannel implements NotificationChannel<SlackMessage> {
    private final SlackProvider provider;
    private final MessageValidator<SlackMessage> validator;
    // Implement send(), getChannelType(), getProviderName()
}
```

4. **Register it:**

```java
NotificationConfig config = NotificationConfig.builder()
    .registerChannel(ChannelType.SLACK, new SlackChannel(
        new SlackWebhookProvider(ProviderConfig.withApiKey("xoxb-xxx"))))
    .build();
```

No existing code needs to change. This is the Open/Closed Principle in action.

### Adding a new provider

Implement the existing provider interface:

```java
public class AmazonSesProvider implements EmailProvider {
    @Override
    public NotificationResult send(EmailMessage message) {
        // Implement SES-specific logic
    }
    
    @Override
    public String getName() {
        return "AmazonSES";
    }
}

// Use it - no changes to EmailChannel needed
EmailChannel channel = new EmailChannel(new AmazonSesProvider(config));
```

---

## Security Best Practices

1. **Never hardcode credentials.** Use environment variables or a secrets manager:

```java
ProviderConfig.builder()
    .apiKey(System.getenv("SENDGRID_API_KEY"))
    .build();
```

2. **API keys are masked in logs.** Providers automatically mask sensitive data (e.g., `SG.x****here`).

3. **Validate before sending.** The library validates all messages before they reach the provider, preventing malformed data from being transmitted.

4. **Use `.gitignore`** to exclude any configuration files that might contain credentials.

5. **ProviderConfig is immutable.** Once created, credentials cannot be modified, reducing the attack surface.

---

## Running with Docker

### Build and run examples

```bash
docker build -t notification-lib .
docker run notification-lib
```

### Run tests inside Docker

```bash
docker build -t notification-lib .
docker run notification-lib ./gradlew test --no-daemon
```

---

## Testing

### Run all tests

```bash
./gradlew test
```

### Test coverage

The library includes **74 unit tests** covering:

- **Validation tests** - Email, SMS, Push message validation (valid/invalid cases)
- **Channel tests** - Email, SMS, Push channel behavior with mocked providers
- **Service tests** - NotificationService routing, error propagation, retry integration
- **Config tests** - Builder validation, channel registration
- **Retry tests** - Exponential backoff calculation, retry exhaustion, callbacks
- **Async tests** - CompletableFuture, batch sending, error handling
- **Event tests** - Pub/Sub publishing, listener isolation, factory methods
- **Template tests** - Variable substitution, missing variables, template registry

All tests use **Mockito** for provider mocking and **AssertJ** for fluent assertions.

---

## Design Decisions

### Why generics for channels?

Using `NotificationChannel<T extends ChannelMessage>` provides compile-time type safety. Email-specific fields (`subject`, `cc`) are guaranteed to be available without casting. The trade-off is slightly more complex signatures, but the safety is worth it for a library API.

### Why Result type instead of only exceptions?

`NotificationResult` allows inspecting outcomes without try-catch for expected failures. Exceptions (`ValidationException`, `SendException`) are reserved for unexpected/programming errors. This hybrid approach gives consumers flexibility.

### Why synchronous event publishing?

Events are published in the same thread as the send operation. This simplifies the mental model and avoids concurrency complexity for listeners. If async event processing is needed, the listener itself can offload to another thread.

### Why not use a framework?

The library is designed to work anywhere Java runs - Spring, Quarkus, Micronaut, plain Java, or even Android. No `@Component`, `@Service`, or classpath scanning required. Just instantiate, configure, and use.

### What's missing / Future improvements

- **Real HTTP integration** - Replace simulated providers with actual HTTP clients (e.g., OkHttp, Java HttpClient)
- **Circuit breaker** - Prevent cascading failures when a provider is down
- **Rate limiting** - Respect provider rate limits (e.g., SendGrid 100 requests/second)
- **Persistence** - Store notification history for auditing
- **Priority queuing** - Send high-priority notifications first
- **Webhook callbacks** - Handle provider delivery status webhooks
- **More providers** - Amazon SES, Vonage, Apple APNs, OneSignal

---

## Tech Stack

- **Java 17+**
- **Gradle** (Kotlin DSL) - Build tool
- **Lombok** - Reduce boilerplate
- **SLF4J + Logback** - Logging
- **Jackson** - JSON support
- **JUnit 5** - Testing
- **Mockito** - Mocking
- **AssertJ** - Fluent assertions

---

## License

This project was created as a technical challenge.

<!-- This project was developed with the assistance of Claude (Anthropic) via Claude Code CLI. The AI was used as a pair programming partner for architecture design discussions, code generation, and iterative debugging. Key decisions about design patterns (Strategy, Builder, Observer), package structure, and SOLID principles were discussed collaboratively before implementation. The developer guided the overall vision and made final decisions on all architectural choices. -->
