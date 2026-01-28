import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.smoke-test")
  id("com.gradleup.shadow")
}

val aiSmokeTest = extensions.getByType(AiSmokeTestExtension::class)

// Create a fat JAR using Shadow instead of Spring Boot plugin for Gradle 9 compatibility
// Spring Boot 2.x doesn't support Gradle 9, and Spring Boot 3.x requires Java 17+
// Shadow creates a simple fat JAR that works with Java 8
tasks.named<ShadowJar>("shadowJar") {
  archiveClassifier.set("")
  mergeServiceFiles()
  
  // Use the standard main class convention for smoke test apps
  manifest {
    attributes["Main-Class"] = "com.microsoft.applicationinsights.smoketestapp.SpringBootApp"
  }
}

aiSmokeTest.testAppArtifactDir.set(tasks.shadowJar.flatMap { it.destinationDirectory })
aiSmokeTest.testAppArtifactFilename.set(tasks.shadowJar.flatMap { it.archiveFileName })
