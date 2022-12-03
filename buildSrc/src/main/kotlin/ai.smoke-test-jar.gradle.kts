import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
  id("ai.smoke-test")
  id("org.springframework.boot")
}

val aiSmokeTest = extensions.getByType(AiSmokeTestExtension::class)

aiSmokeTest.testAppArtifactDir.set(tasks.named<BootJar>("bootJar").get().destinationDirectory.get())
aiSmokeTest.testAppArtifactFilename.set(tasks.named<BootJar>("bootJar").get().archiveFileName.get())
