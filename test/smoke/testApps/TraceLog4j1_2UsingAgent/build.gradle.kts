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

  compileOnly("javax.servlet:javax.servlet-api:3.0.1")

  implementation("log4j:log4j:1.2.17")
}
