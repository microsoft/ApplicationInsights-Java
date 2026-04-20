import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("ai.smoke-test-jar")
}

configurations.configureEach {
  resolutionStrategy.force(
    "ch.qos.logback:logback-classic:1.5.21",
    "ch.qos.logback:logback-core:1.5.21",
    "org.slf4j:slf4j-api:2.0.17"
  )
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:4.0.0")
  implementation("org.springframework.boot:spring-boot-starter-actuator:4.0.0")
  implementation("org.springframework.boot:spring-boot-starter-micrometer-metrics:4.0.0")
}

// Spring Boot 4 splits auto-configuration across many module JARs, each with its own
// META-INF/spring/AutoConfiguration.imports file. Shadow's default behavior keeps only
// one copy, losing most entries. We provide a pre-merged resource file in
// src/main/resources/META-INF/spring/ and use append() to prevent any single
// dependency copy from overwriting it.
tasks.named<ShadowJar>("shadowJar") {
  append("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
}
