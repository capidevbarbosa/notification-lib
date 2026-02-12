plugins {
    `java-library`
    jacoco
}

group = "com.notification.lib"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Lombok - reduce boilerplate
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

    // Logging - only the API for the library; consumers provide their own implementation
    implementation("org.slf4j:slf4j-api:2.0.16")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.12")

    // Jackson - JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    // Exclude simulated providers and examples from coverage reports
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude(
                "**/examples/**",
                "**/provider/SendGridProvider*",
                "**/provider/MailgunProvider*",
                "**/provider/TwilioSmsProvider*",
                "**/provider/FirebasePushProvider*"
            )
        }
    }))
}

tasks.jacocoTestCoverageVerification {
    // Apply same exclusions as the report
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude(
                "**/examples/**",
                "**/provider/SendGridProvider*",
                "**/provider/MailgunProvider*",
                "**/provider/TwilioSmsProvider*",
                "**/provider/FirebasePushProvider*"
            )
        }
    }))
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

// Configuration for running examples (logback needed only here, not in the published library)
val examples by configurations.creating {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    examples("ch.qos.logback:logback-classic:1.5.12")
}

// Separate task for running examples (not included in library jar)
tasks.register<JavaExec>("runExamples") {
    description = "Runs the notification library examples"
    mainClass.set("com.notification.lib.examples.NotificationExamples")
    classpath = sourceSets["main"].output + examples
}
