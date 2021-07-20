plugins {
  id("ai.java-conventions")
  id("war")
}

war {
  // this is done to remove the version from the archive file name
  // to make span name verification simpler
  archiveFileName = project.name + ".war"
}

ext.testAppArtifactDir = war.destinationDirectory
ext.testAppArtifactFilename = project.name + ".war"

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web-auto")
  implementation("org.springframework.boot:spring-boot-starter-web:1.3.8.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }

  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
}
