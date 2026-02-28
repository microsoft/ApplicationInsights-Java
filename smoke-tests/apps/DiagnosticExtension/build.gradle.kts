plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation(project(":agent:agent-profiler:agent-diagnostics-api"))
  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  // MockExtension is loaded as a separate agent extension, not bundled in the app
  compileOnly(project(":smoke-tests:apps:DiagnosticExtension:MockExtension"))
  testImplementation(project(":smoke-tests:framework"))
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12")
}
