plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("io.opentelemetry:opentelemetry-api:1.12.0")
}
