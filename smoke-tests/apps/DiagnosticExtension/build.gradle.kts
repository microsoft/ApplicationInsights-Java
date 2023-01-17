plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation(project(mapOf("path" to ":agent:agent-profiler:agent-diagnostics-api")))
  implementation(project(mapOf("path" to ":agent:agent-profiler:agent-alerting-api")))
  implementation(project(mapOf("path" to ":smoke-tests:apps:DiagnosticExtension:MockExtension")))
  testImplementation(project(mapOf("path" to ":smoke-tests:framework")))
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE")
}
