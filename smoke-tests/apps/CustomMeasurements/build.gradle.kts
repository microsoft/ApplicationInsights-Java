plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12")
  implementation("io.opentelemetry:opentelemetry-api")
  compileOnly(project(":smoke-tests:apps:CustomMeasurements:TestExtension"))
}
