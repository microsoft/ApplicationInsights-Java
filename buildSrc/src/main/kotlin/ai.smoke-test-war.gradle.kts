import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.smoke-test")
  id("war")
}

tasks.war {
  // this is done to remove the version from the archive file name
  // to make span name verification simpler
  archiveFileName.set(project.name + ".war")
}

val aiSmokeTest = extensions.getByType(AiSmokeTestExtension::class)

aiSmokeTest.testAppArtifactDir.set(tasks.getByName<War>("war").destinationDirectory.get())
aiSmokeTest.testAppArtifactFilename.set(project.name + ".war")

dependencies {
  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
}
