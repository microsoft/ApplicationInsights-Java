plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation("io.opentelemetry:opentelemetry-api:1.0.0")
  implementation("io.opentelemetry:opentelemetry-extension-annotations:1.0.0")
}
