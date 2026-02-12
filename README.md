# Notification Library

Una libreria de notificaciones para Java **independiente de frameworks** (*framework-agnostic*) que proporciona una API unificada para enviar notificaciones a traves de multiples canales (Email, SMS, Notificaciones Push). Disenada con la extensibilidad en mente: cambia de proveedores o anade nuevos canales sin modificar el codigo existente.

## Tabla de Contenidos

* [Instalacion](#instalacion)
* [Inicio Rapido](#inicio-rapido)
* [Arquitectura](#arquitectura)
* [Configuracion](#configuracion)
* [Canales y Proveedores](#canales-y-proveedores)
* [Funcionalidades](#funcionalidades)
* [Configuracion Avanzada](#configuracion-avanzada)
* [Extender la Libreria](#extender-la-libreria)
* [Pruebas](#pruebas)
* [Integracion Continua](#integracion-continua)
* [Mejores Practicas de Seguridad](#mejores-practicas-de-seguridad)
* [Ejecucion con Docker](#ejecucion-con-docker)
* [Decisiones de Diseno](#decisiones-de-diseno)

---

## Instalacion

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

El JAR se generara en `build/libs/notification-lib-1.0.0.jar`.

> **Nota:** La libreria solo incluye `slf4j-api` como dependencia de logging. Los consumidores deben proveer su propia implementacion (Logback, Log4j2, etc.) en su proyecto.

---

## Inicio Rapido

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

// 4. Enviar una notificacion
NotificationResult result = service.send(EmailMessage.builder()
    .recipient("user@example.com")
    .from("noreply@myapp.com")
    .subject("Bienvenido!")
    .body("Hola, bienvenido a nuestra plataforma!")
    .build());

if (result.isSuccess()) {
    System.out.println("Enviado! ID: " + result.getMessageId());
}
```

---

## Arquitectura

### Estructura de Paquetes

```
com.notification.lib
├── core/           # Interfaces y clases centrales (NotificationService, NotificationChannel, etc.)
├── channel/
│   ├── email/      # Canal de email + proveedores (SendGrid, Mailgun)
│   ├── sms/        # Canal de SMS + proveedores (Twilio)
│   └── push/       # Canal de push + proveedores (Firebase)
├── config/         # Configuracion central (NotificationConfig, ProviderConfig)
├── event/          # Sistema de eventos pub/sub (NotificationEventPublisher)
├── retry/          # Mecanismo de reintentos (RetryExecutor, AsyncRetryExecutor)
├── validation/     # Validadores por canal (EmailMessageValidator, etc.)
├── template/       # Motor de plantillas para mensajes
├── exception/      # Excepciones tipadas (ValidationException, SendException)
└── examples/       # Ejemplos de uso
```

### Patrones de Diseno

| Patron | Donde se usa | Proposito |
| :--- | :--- | :--- |
| **Strategy** | `NotificationChannel<T>` | Cada canal es una estrategia de envio intercambiable. |
| **Builder** | Mensajes, Config, Resultados | Construccion de objetos fluida y segura en tipos. |
| **Facade** | `NotificationService` | Punto de entrada unico para el subsistema de notificaciones. |
| **Observer** | `NotificationEventPublisher` | Desacopla el envio del seguimiento de estados. |
| **Template Method** | Implementaciones de canales | Flujo: Validar -> Enviar -> Retornar resultado. |
| **Factory** | `RetryExecutorFactory` | Permite inyectar estrategias de retry custom. |

### Principios SOLID

* **S - Responsabilidad Unica:** Cada clase tiene una sola razon para cambiar (`EmailChannel` envia, `EmailMessageValidator` valida, `RetryExecutor` reintenta).
* **O - Abierto/Cerrado:** Anade nuevos canales implementando `NotificationChannel<T>` sin modificar el codigo existente.
* **L - Sustitucion de Liskov:** Todas las implementaciones de canales son intercambiables donde se espera un `NotificationChannel<T>`.
* **I - Segregacion de Interfaces:** Interfaces pequenas y enfocadas (`MessageValidator`, `NotificationListener`, `EmailProvider`).
* **D - Inversion de Dependencias:** `NotificationService` depende de abstracciones (`NotificationChannel`, `RetryExecutorFactory`, `NotificationEventPublisher`), no de implementaciones concretas. Todas las dependencias son inyectables via `NotificationConfig`.

---

## Configuracion

Toda la configuracion se realiza mediante codigo Java. No se requiere configuracion basada en YAML, propiedades o anotaciones.

### Configuracion multicanal

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
    
    // Opcional: politica de reintentos
    .withRetryPolicy(RetryPolicy.builder()
        .maxRetries(3)
        .initialDelayMs(1000)
        .multiplier(2.0)
        .build())
    
    // Opcional: oyentes de eventos
    .addListener(event -> log.info("Notificacion {}: {}", event.getMessageId(), event.getStatus()))
    
    .build();

NotificationService service = new NotificationService(config);
```

---

## Canales y Proveedores

### Email

| Proveedor | Clase | Configuracion requerida |
| :--- | :--- | :--- |
| SendGrid | `SendGridProvider` | `apiKey` |
| Mailgun | `MailgunProvider` | `apiKey`, `domain` |

### SMS

| Proveedor | Clase | Configuracion requerida |
| :--- | :--- | :--- |
| Twilio | `TwilioSmsProvider` | `apiKey` (Account SID), `apiSecret` (Auth Token) |

### Push

| Proveedor | Clase | Configuracion requerida |
| :--- | :--- | :--- |
| Firebase FCM | `FirebasePushProvider` | `apiKey`, `properties.projectId` |

---

## Funcionalidades

### Notificaciones Asincronas

```java
AsyncNotificationService asyncService = new AsyncNotificationService(service);

// Envio asincrono individual
CompletableFuture<NotificationResult> future = asyncService.sendAsync(emailMessage);
future.thenAccept(result -> System.out.println("Enviado: " + result.getMessageId()))
      .exceptionally(ex -> { log.error("Fallo", ex); return null; });

// Envio batch concurrente
CompletableFuture<List<NotificationResult>> batchFuture = asyncService.sendBatch(messages);

// Envio batch tolerante a fallos (no falla si una notificacion individual falla)
CompletableFuture<List<NotificationResult>> settledFuture = asyncService.sendBatchSettled(messages);
```

> **Nota:** El constructor por defecto usa `Executors.newCachedThreadPool()`. Para Java 21+, se puede pasar `Executors.newVirtualThreadPerTaskExecutor()` como executor custom.

### Mecanismo de Reintentos

Retroceso exponencial (*exponential backoff*) con parametros configurables:

* Secuencia de reintento: 1s -> 2s -> 4s (luego desiste).
* Solo `SendException` activa reintentos. `ValidationException` falla inmediatamente.

**Retry bloqueante (por defecto):** `RetryExecutor` usa `Thread.sleep()` para el backoff. Adecuado para envios sincronos.

**Retry no bloqueante:** `AsyncRetryExecutor` usa `ScheduledExecutorService` para delays sin bloquear hilos. Ideal para escenarios async de alto throughput:

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

NotificationConfig config = NotificationConfig.builder()
    .registerChannel(ChannelType.EMAIL, emailChannel)
    .withRetryPolicy(RetryPolicy.defaultPolicy())
    .withRetryExecutorFactory((policy, callback) ->
        new AsyncRetryExecutor(policy, scheduler, callback).asBlockingExecutor())
    .build();
```

### Sistema de Eventos

```java
NotificationConfig config = NotificationConfig.builder()
    .registerChannel(ChannelType.EMAIL, emailChannel)
    .addListener(event -> {
        switch (event.getStatus()) {
            case PENDING  -> log.info("Preparando envio...");
            case SENDING  -> log.info("Enviando...");
            case SENT     -> log.info("Enviado exitosamente!");
            case FAILED   -> log.error("Fallo: {}", event.getErrorMessage());
            case RETRYING -> log.warn("Reintento #{}", event.getRetryAttempt());
        }
    })
    .build();
```

### Motor de Plantillas

```java
TemplateEngine engine = new TemplateEngine();
engine.register("welcome", new MessageTemplate("welcome",
    "Hola {{name}}, tu pedido {{orderId}} esta listo!"));

String rendered = engine.render("welcome", Map.of(
    "name", "Juan",
    "orderId", "12345"
));
// Resultado: "Hola Juan, tu pedido 12345 esta listo!"
```

---

## Configuracion Avanzada

### Inyeccion de Dependencias (DIP)

`NotificationService` soporta inyeccion de todas sus dependencias internas para mejorar testabilidad y flexibilidad:

```java
// Inyectar un event publisher custom
NotificationEventPublisher customPublisher = new NotificationEventPublisher();
customPublisher.addListener(myMetricsListener);

NotificationConfig config = NotificationConfig.builder()
    .registerChannel(ChannelType.EMAIL, emailChannel)
    .withEventPublisher(customPublisher)           // Custom publisher
    .withRetryExecutorFactory(myCustomFactory)      // Custom retry strategy
    .build();
```

Si no se configuran, se usan los defaults (publisher construido desde los listeners registrados, y `RetryExecutor` bloqueante estandar).

### Inmutabilidad y Thread-Safety

* `NotificationConfig` almacena canales y listeners en colecciones inmutables.
* `ProviderConfig.getProperties()` retorna una vista inmutable del mapa.
* `NotificationEventPublisher` usa `CopyOnWriteArrayList` para thread-safety.
* `TemplateEngine` usa `ConcurrentHashMap` para registro concurrente.

---

## Extender la Libreria

### Agregar un nuevo canal de notificacion

Para agregar un canal (ej: WhatsApp):

1. **Agregar valor al enum** `ChannelType`:
   ```java
   public enum ChannelType {
       EMAIL, SMS, PUSH, SLACK, WHATSAPP  // <-- nuevo
   }
   ```

2. **Crear la clase de mensaje** extendiendo `ChannelMessage`:
   ```java
   @Getter @SuperBuilder
   public class WhatsAppMessage extends ChannelMessage {
       private final String templateId;
       @Override public ChannelType getChannelType() { return ChannelType.WHATSAPP; }
   }
   ```

3. **Crear la interfaz del proveedor:**
   ```java
   public interface WhatsAppProvider {
       NotificationResult send(WhatsAppMessage message);
       String getName();
   }
   ```

4. **Implementar el proveedor** (ej: `TwilioWhatsAppProvider`).

5. **Crear el canal** implementando `NotificationChannel<WhatsAppMessage>`.

6. **Crear el validador** implementando `MessageValidator<WhatsAppMessage>`.

7. **Registrar** en la configuracion:
   ```java
   config.registerChannel(ChannelType.WHATSAPP, whatsAppChannel);
   ```

> El core (`NotificationService`, `NotificationChannel`) no requiere modificaciones.

### Agregar un nuevo proveedor a un canal existente

Solo implementa la interfaz del proveedor (ej: `EmailProvider`) y pasalo al canal:

```java
EmailChannel channel = new EmailChannel(new MyCustomEmailProvider(config));
```

---

## Pruebas

### Ejecutar tests

```bash
./gradlew test
```

### Ejecutar tests con reporte de cobertura

```bash
./gradlew test jacocoTestReport
```

El reporte HTML se genera en `build/reports/jacoco/test/html/index.html`.

### Verificar cobertura minima (70%)

```bash
./gradlew jacocoTestCoverageVerification
```

### Estructura de tests

| Categoria | Tests | Que verifica |
| :--- | :--- | :--- |
| **Validadores** | `EmailMessageValidatorTest`, `SmsMessageValidatorTest`, `PushMessageValidatorTest` | Validacion de campos por canal |
| **Canales** | `EmailChannelTest`, `SmsChannelTest`, `PushChannelTest` | Flujo validar -> enviar -> resultado |
| **Contrato de canal** | `ChannelContractTest` | Que los 3 canales respetan el mismo contrato conductual |
| **Core** | `NotificationServiceTest`, `DependencyInversionTest` | Orquestacion, routing, DIP |
| **Async** | `AsyncNotificationServiceTest`, `AsyncNotificationServiceConcurrencyTest` | Envio async, batch, concurrencia real |
| **Retry** | `RetryExecutorTest`, `AsyncRetryExecutorTest` | Backoff exponencial, callbacks, retry no bloqueante |
| **Eventos** | `NotificationEventPublisherTest`, `NotificationEventPublisherNullSafetyTest` | Pub/sub, fault isolation, null-safety |
| **Config** | `NotificationConfigTest`, `ProviderConfigImmutabilityTest` | Builder, inmutabilidad |
| **Templates** | `TemplateEngineTest` | Renderizado de plantillas |

### Ejecutar los ejemplos

```bash
./gradlew runExamples
```

---

## Integracion Continua

El proyecto incluye un pipeline de CI con GitHub Actions (`.github/workflows/ci.yml`) que ejecuta automaticamente en cada push/PR a `main`:

1. Build del proyecto con Java 17
2. Ejecucion de todos los tests
3. Generacion de reporte de cobertura (JaCoCo)
4. Verificacion de cobertura minima (70%)
5. Publicacion de artefactos (reportes de tests y cobertura)

---

## Mejores Practicas de Seguridad

* **Nunca** incluir API keys en el codigo fuente. Usar variables de entorno o un gestor de secretos.
* `ProviderConfig.getProperties()` retorna una vista inmutable para prevenir mutacion accidental.
* Las API keys se enmascaran en los logs de los proveedores simulados.

---

## Ejecucion con Docker

```bash
docker build -t notification-lib .
docker run notification-lib
```

---

## Decisiones de Diseno

* **Por que genericos para los canales?** El uso de `NotificationChannel<T extends ChannelMessage>` proporciona seguridad de tipos en tiempo de compilacion. Los campos especificos de Email (asunto, cc) estan garantizados sin necesidad de casting.
* **Por que tipo Result en lugar de solo excepciones?** `NotificationResult` permite inspeccionar los resultados sin bloques try-catch para fallos esperados.
* **Por que no usar un framework?** La libreria esta disenada para funcionar donde sea que corra Java: Spring, Quarkus, Micronaut, Java puro o incluso Android.
* **Por que inyeccion via Config y no via constructor?** Mantiene la API simple (un solo parametro en el constructor) mientras permite personalizacion profunda via el builder de `NotificationConfig`.
* **Por que retry bloqueante por defecto?** Es mas simple y predecible. Se documenta la limitacion y se ofrece `AsyncRetryExecutor` como alternativa no bloqueante.
* **Por que solo `slf4j-api` sin implementacion?** Una libreria no debe imponer la implementacion de logging. Los consumidores eligen (Logback, Log4j2, etc.).

---

## Licencia

Este proyecto fue creado como un desafio tecnico.
