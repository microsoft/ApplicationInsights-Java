plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation("io.opentelemetry:opentelemetry-api:1.12.0")

  smokeTestImplementation("org.mock-server:mockserver-netty:5.15.0:shaded")
  smokeTestImplementation("org.awaitility:awaitility:4.2.0")
  smokeTestImplementation("io.opentelemetry.proto:opentelemetry-proto:0.14.0-alpha")
}

configurations.all {
  resolutionStrategy.force("io.opentelemetry:opentelemetry-api:1.12.0")
}
