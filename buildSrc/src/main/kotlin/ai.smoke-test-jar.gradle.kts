import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.smoke-test")
  id("org.springframework.boot")
}

configurations.all {
  // spring boot 2.x requires slf4j 1.x
  val slf4jVersion = "1.7.36"
  resolutionStrategy.force("org.slf4j:slf4j-api:${slf4jVersion}")
  resolutionStrategy.force("org.slf4j:log4j-over-slf4j:${slf4jVersion}")
  resolutionStrategy.force("org.slf4j:jcl-over-slf4j:${slf4jVersion}")
  resolutionStrategy.force("org.slf4j:jul-to-slf4j:${slf4jVersion}")
  resolutionStrategy.force("ch.qos.logback:logback-classic:1.2.12")
}

val aiSmokeTest = extensions.getByType(AiSmokeTestExtension::class)

aiSmokeTest.testAppArtifactDir.set(tasks.getByName<Jar>("bootJar").destinationDirectory.get())
aiSmokeTest.testAppArtifactFilename.set(tasks.getByName<Jar>("bootJar").archiveFileName.get())
