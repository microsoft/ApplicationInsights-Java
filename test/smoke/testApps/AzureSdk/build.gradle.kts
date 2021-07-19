plugins {
  id("ai.smoke-tests")
  id("war")
}

tasks.war {
  // this is done to remove the version from the archive file name
  // to make span name verification simpler
  archiveFileName.set(project.name + ".war")
}

aiSmokeTest.testAppArtifactDir.set(tasks.war.get().destinationDirectory.get())
aiSmokeTest.testAppArtifactFilename.set(project.name + ".war")

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation("com.azure:azure-core:1.14.0")

  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
}
