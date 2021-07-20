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
  implementation("com.microsoft.azure:applicationinsights-web")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:${versions.aiLegacySdk}") {
    // applicationinsights-core is embedded in applicationinsights-web
    // and duplicate class files produces lots of warning messages on jetty
    exclude("com.microsoft.azure", "applicationinsights-core")
  }

  compileOnly("javax.servlet:javax.servlet-api:3.0.1")

  implementation("ch.qos.logback:logback-classic:1.2.3")
}
