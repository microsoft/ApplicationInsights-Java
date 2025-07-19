import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.smoke-test")
  id("org.springframework.boot")
}

val aiSmokeTest = extensions.getByType(AiSmokeTestExtension::class)

aiSmokeTest.testAppArtifactDir.set(tasks.named<Jar>("bootJar").flatMap { it.destinationDirectory })
aiSmokeTest.testAppArtifactFilename.set(tasks.named<Jar>("bootJar").flatMap { it.archiveFileName })
