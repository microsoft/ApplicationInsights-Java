plugins {
  id("ai.java-conventions")
}

// Allows publishing this library to the local host ONLY if -Ppublish-diagnostics is provided
if (project.properties.containsKey("publish-diagnostics")) {
  apply(plugin = "ai.publish-conventions")
}

dependencies {
  compileOnly("com.fasterxml.jackson.core:jackson-annotations")
  compileOnly("com.fasterxml.jackson.core:jackson-databind")
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")
}
