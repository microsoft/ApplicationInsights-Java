plugins {
  id("ai.java-conventions")
}

// Allows publishing this library to the local host ONLY if -Ppublish-diagnostics is provided
if (project.properties.containsKey("publish-diagnostics")) {
  apply(plugin = "ai.publish-conventions")
}

dependencies {
  implementation(project(":agent:agent-profiler:agent-alerting-api"))

  compileOnly("org.slf4j:slf4j-api")
  compileOnly("com.fasterxml.jackson.core:jackson-annotations")
  compileOnly("com.fasterxml.jackson.core:jackson-databind")

  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")
}
