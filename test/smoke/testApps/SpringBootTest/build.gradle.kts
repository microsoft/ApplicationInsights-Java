plugins {
  id("ai.smoke-test")
  id "org.springframework.boot" version "2.1.7.RELEASE"
  id("war")
}

dependencies {
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter")
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  // this dependency is needed to make wildfly happy
  implementation("org.reactivestreams:reactive-streams:1.0.3")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")

  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
}

configurations {
  smokeTestImplementation.exclude group: "org.springframework.boot"
  smokeTestRuntime.exclude group: "org.springframework.boot"
}

bootWar {
  // this is done to remove the version from the archive file name
  // to make span name verification simpler
  archiveFileName = project.name + ".war"
}

val aiSmokeTest = extensions.getByType(com.microsoft.applicationinsights.gradle.AiSmokeTestExtension::class)

aiSmokeTest.testAppArtifactDir.set(tasks.war.get().destinationDirectory.get())
aiSmokeTest.testAppArtifactFilename.set(project.name + ".war")
