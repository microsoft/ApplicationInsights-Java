plugins {
  `java`
}

dependencies {
  implementation(project(mapOf("path" to ":agent:agent-profiler:agent-diagnostics-api")))
  implementation(project(mapOf("path" to ":agent:agent-profiler:agent-alerting-api")))
}

tasks.jar {
  archiveFileName.set("extension.jar")
}
