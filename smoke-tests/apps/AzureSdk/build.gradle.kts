plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  // want to test with one of the earliest version supported, and not managed version used in agent
  implementation("com.azure:azure-core:1.39.0")
}
