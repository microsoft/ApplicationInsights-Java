import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.smoke-test")
  id("org.springframework.boot")
}

val aiSmokeTest = extensions.getByType(AiSmokeTestExtension::class)

aiSmokeTest.testAppArtifactDir.set(tasks.getByName<Jar>("bootJar").destinationDirectory.get())
aiSmokeTest.testAppArtifactFilename.set(tasks.getByName<Jar>("bootJar").archiveFileName.get())
