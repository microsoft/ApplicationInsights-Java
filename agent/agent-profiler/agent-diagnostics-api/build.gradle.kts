plugins {
  id("ai.java-conventions")
}

// Allows publishing this library to the local host ONLY if -Ppublish-diagnostics is provided
if (project.properties.containsKey("publish-diagnostics")) {
  apply(plugin = "ai.publish-conventions")
}

dependencies {
  implementation(project(":agent:agent-profiler:agent-alerting-api"))
}
