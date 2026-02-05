import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
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
  
  // Properly merge spring.factories files from all dependencies
  // This is required for Spring Boot auto-configuration to work
  // Use PropertiesFileTransformer to merge duplicate keys instead of simple append
  transform(PropertiesFileTransformer::class.java) {
    paths = listOf("META-INF/spring.factories")
    mergeStrategy = "append"
  }
  
  // Set main class - can be overridden by individual projects via mainClassName property
  manifest {
    val mainClass = if (project.hasProperty("mainClassName")) {
      project.property("mainClassName") as String
    } else {
      // Default main class for most smoke test apps
      "com.microsoft.applicationinsights.smoketestapp.SpringBootApp"
    }
    attributes["Main-Class"] = mainClass
  }
}

// Make jar task depend on shadowJar and use the shadow JAR output
// This prevents the regular jar task from overwriting the fat JAR
tasks.named<Jar>("jar") {
  dependsOn(tasks.shadowJar)
  enabled = false
}

aiSmokeTest.testAppArtifactDir.set(tasks.shadowJar.flatMap { it.destinationDirectory })
aiSmokeTest.testAppArtifactFilename.set(tasks.shadowJar.flatMap { it.archiveFileName })
