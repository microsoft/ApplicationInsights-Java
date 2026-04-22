import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.smoke-test")
  id("org.springframework.boot") version "4.0.0"
}

// The workspace-wide dependencyManagement module pins logback-classic to 1.3.16 (Java 8
// compatible). Spring Boot 4 starters don't declare a logback version directly (they rely
// on Spring Boot's BOM, which we don't apply here -- applying io.spring.dependency-management
// would also downgrade Jetty used by the smoke-test framework), so the 1.3.16 constraint
// wins and fails at startup with AbstractMethodError against the SB4 RootLogLevelConfigurator.
// Force the logback 1.5.x line required by Spring Boot 4.
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
