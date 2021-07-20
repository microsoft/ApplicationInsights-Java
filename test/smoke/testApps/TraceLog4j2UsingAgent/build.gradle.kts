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
  implementation("org.apache.logging.log4j:log4j-api:2.11.0")
  implementation("org.apache.logging.log4j:log4j-core:2.11.0")
  implementation("com.microsoft.azure:applicationinsights-web-auto")

  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
}
