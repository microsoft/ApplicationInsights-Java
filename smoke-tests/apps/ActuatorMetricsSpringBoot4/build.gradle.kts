import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.smoke-test")
  id("org.springframework.boot") version "4.0.0"
}

// The ai.smoke-test convention plugin applies `resolutionStrategy.force` on every
// configuration to pin logback-classic to 1.2.12 and slf4j-api to 1.7.36 (needed by the
// older Spring Boot smoke-test apps). Spring Boot 4 requires logback 1.5.x and slf4j 2.x
// and fails at startup with AbstractMethodError against SB4's RootLogLevelConfigurator
// otherwise. Re-force the newer versions here to override the convention's force.
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
