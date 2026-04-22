import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.smoke-test")
  id("org.springframework.boot") version "4.0.0"
}

// Override the workspace-wide dependencyManagement pin of logback 1.3.x (Java 8 target)
// so Spring Boot 4 can resolve its required logback 1.5.x (requires Java 17, which this
// app already targets).
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

val aiSmokeTest = extensions.getByType(AiSmokeTestExtension::class)
aiSmokeTest.testAppArtifactDir.set(tasks.bootJar.flatMap { it.destinationDirectory })
aiSmokeTest.testAppArtifactFilename.set(tasks.bootJar.flatMap { it.archiveFileName })
