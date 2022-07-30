import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.smoke-test")
  id("org.springframework.boot")
}

val aiSmokeTest = extensions.getByType(AiSmokeTestExtension::class)

aiSmokeTest.testAppArtifactDir.set(tasks.jar.get().destinationDirectory.get())
aiSmokeTest.testAppArtifactFilename.set(tasks.jar.get().archiveFileName.get())
