import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension
import org.gradle.api.file.DuplicatesStrategy

plugins {
  id("ai.smoke-test")
  id("com.gradleup.shadow")
}

val aiSmokeTest = extensions.getByType(AiSmokeTestExtension::class)

aiSmokeTest.mainClass.convention("com.microsoft.applicationinsights.smoketestapp.SpringBootApp")

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
  archiveClassifier.set("")
  archiveVersion.set("")
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

  mergeServiceFiles()
  mergeServiceFiles("META-INF/spring.factories")
  mergeServiceFiles("META-INF/spring.handlers")
  mergeServiceFiles("META-INF/spring.schemas")
  mergeServiceFiles("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")

  manifest {
    attributes["Main-Class"] = aiSmokeTest.mainClass.get()
  }
}

tasks.named("assemble") {
  dependsOn(shadowJar)
}

aiSmokeTest.testAppArtifactDir.set(shadowJar.flatMap { it.destinationDirectory })
aiSmokeTest.testAppArtifactFilename.set(shadowJar.flatMap { it.archiveFileName })
