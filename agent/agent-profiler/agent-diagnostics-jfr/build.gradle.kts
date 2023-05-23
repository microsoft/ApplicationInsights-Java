plugins {
  id("ai.java-conventions")
}

// Allows publishing this library to the local host ONLY if -Ppublish-diagnostics is provided
if (project.properties.containsKey("publish-diagnostics")) {
  apply(plugin = "ai.publish-conventions")
}

dependencies {
  compileOnly("org.gradle.jfr.polyfill:jfr-polyfill:1.0.0")

  compileOnly("com.fasterxml.jackson.core:jackson-annotations")

  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")
}
