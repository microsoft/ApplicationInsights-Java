plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation(project(":smoke-tests:apps:Diagnostics:JfrFileReader"))
  implementation(project(":agent:agent-profiler:agent-diagnostics-api"))
  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  testImplementation(project(":smoke-tests:framework"))
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE")
}
