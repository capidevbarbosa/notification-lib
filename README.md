# Notification Library

Una librería de notificaciones para Java **independiente de frameworks** (*framework-agnostic*) que proporciona una API unificada para enviar notificaciones a través de múltiples canales (Email, SMS, Notificaciones Push). Diseñada con la extensibilidad en mente: cambia de proveedores o añade nuevos canales sin modificar el código existente.

## Tabla de Contenidos

* [Instalación](#instalación)
* [Inicio Rápido](#inicio-rápido)
* [Arquitectura](#arquitectura)
* [Configuración](#configuración)
* [Canales y Proveedores](#canales-y-proveedores)
* [Funcionalidades](#funcionalidades)
* [Referencia de la API](#referencia-de-la-api)
* [Extender la Librería](#extender-la-librería)
* [Mejores Prácticas de Seguridad](#mejores-prácticas-de-seguridad)
* [Ejecución con Docker](#ejecución-con-docker)
* [Pruebas](#pruebas)
* [Decisiones de Diseño](#decisiones-de-diseño)

---

## Instalación

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

### Construir desde la fuente

```bash
git clone <repository-url>
cd notification-lib
./gradlew build
```

El JAR se generará en `build/libs/notification-lib-1.0.0.jar`.

---

## Inicio Rápido

```java
import com.notification.lib.channel.email.*;
import com.notification.lib.channel.email.provider.*;
import com.notification.lib.config.*;
import com.notification.lib.core.*;

// 1. Configurar el proveedor
ProviderConfig sendGridConfig = ProviderConfig.builder()
    .apiKey(System.getenv("SENDGRID_API_KEY"))
    .build();

// 2. Crear el canal con el proveedor
EmailChannel emailChannel = new EmailChannel(new SendGridProvider(sendGridConfig));

// 3. Configurar el servicio
NotificationConfig config = NotificationConfig.builder()
    .registerChannel(ChannelType.EMAIL, emailChannel)
    .build();

NotificationService service = new NotificationService(config);

// 4. Enviar una notificación
NotificationResult result = service.send(EmailMessage.builder()
    .recipient("user@example.com")
    .from("noreply@myapp.com")
    .subject("¡Bienvenido!")
    .body("Hola, ¡bienvenido a nuestra plataforma!")
    .build());

if (result.isSuccess()) {
    System.out.println("¡Enviado! ID: " + result.getMessageId());
}
```

---

## Arquitectura



### Patrones de Diseño

| Patrón | Dónde se usa | Propósito |
| :--- | :--- | :--- |
| **Strategy** | `NotificationChannel<T>` | Cada canal es una estrategia de envío intercambiable. |
| **Builder** | Mensajes, Config, Resultados | Construcción de objetos fluida y segura en tipos. |
| **Facade** | `NotificationService` | Punto de entrada único para el subsistema de notificaciones. |
| **Observer** | `NotificationEventPublisher` | Desacopla el envío del seguimiento de estados. |
| **Template Method** | Implementaciones de canales | Flujo: Validar -> Enviar -> Retornar resultado. |

### Principios SOLID

* **S - Responsabilidad Única:** Cada clase tiene una sola razón para cambiar (`EmailChannel` envía, `EmailMessageValidator` valida).
* **O - Abierto/Cerrado:** Añade nuevos canales implementando `NotificationChannel<T>` sin modificar el código existente.
* **L - Sustitución de Liskov:** Todas las implementaciones de canales son intercambiables donde se espera un `NotificationChannel<T>`.
* **I - Segregación de Interfaces:** Interfaces pequeñas y enfocadas (`MessageValidator`, `NotificationListener`, `EmailProvider`).
* **D - Inversión de Dependencias:** `NotificationService` depende de abstracciones (`NotificationChannel`), no de proveedores concretos.

---

## Configuración

Toda la configuración se realiza mediante código Java. No se requiere configuración basada en YAML, propiedades o anotaciones.

### Configuración multicanal

```java
NotificationConfig config = NotificationConfig.builder()
    // Email vía SendGrid
    .registerChannel(ChannelType.EMAIL, new EmailChannel(
        new SendGridProvider(ProviderConfig.builder()
            .apiKey(System.getenv("SENDGRID_API_KEY"))
            .build())))
    
    // SMS vía Twilio
    .registerChannel(ChannelType.SMS, new SmsChannel(
        new TwilioSmsProvider(ProviderConfig.builder()
            .apiKey(System.getenv("TWILIO_ACCOUNT_SID"))
            .apiSecret(System.getenv("TWILIO_AUTH_TOKEN"))
            .build())))
    
    // Push vía Firebase
    .registerChannel(ChannelType.PUSH, new PushChannel(
        new FirebasePushProvider(ProviderConfig.builder()
            .apiKey(System.getenv("FIREBASE_SERVER_KEY"))
            .properties(Map.of("projectId", "my-project"))
            .build())))
    
    // Opcional: política de reintentos
    .withRetryPolicy(RetryPolicy.builder()
        .maxRetries(3)
        .initialDelayMs(1000)
        .multiplier(2.0)
        .build())
    
    // Opcional: oyentes de eventos
    .addListener(event -> log.info("Notificación {}: {}", event.getMessageId(), event.getStatus()))
    
    .build();

NotificationService service = new NotificationService(config);
```

---

## Funcionalidades

### Notificaciones Asíncronas

```java
AsyncNotificationService asyncService = new AsyncNotificationService(service);

// Envío asíncrono individual
CompletableFuture<NotificationResult> future = asyncService.sendAsync(emailMessage);
future.thenAccept(result -> System.out.println("Enviado: " + result.getMessageId()))
      .exceptionally(ex -> { log.error("Falló", ex); return null; });
```

### Mecanismo de Reintentos

Retroceso exponencial (*exponential backoff*) con parámetros configurables:
* Secuencia de reintento: 1s -> 2s -> 4s (luego desiste).
* Solo `SendException` activa reintentos. `ValidationException` falla inmediatamente.

---

## Decisiones de Diseño

* **¿Por qué genéricos para los canales?** El uso de `NotificationChannel<T extends ChannelMessage>` proporciona seguridad de tipos en tiempo de compilación. Los campos específicos de Email (asunto, cc) están garantizados sin necesidad de casting.
* **¿Por qué tipo Result en lugar de solo excepciones?** `NotificationResult` permite inspeccionar los resultados sin bloques try-catch para fallos esperados.
* **¿Por qué no usar un framework?** La librería está diseñada para funcionar donde sea que corra Java: Spring, Quarkus, Micronaut, Java puro o incluso Android.

---

## Licencia

Este proyecto fue creado como un desafío técnico.
