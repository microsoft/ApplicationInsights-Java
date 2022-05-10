plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE")
  implementation("io.opentelemetry:opentelemetry-api:1.12.0")
}
