plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation("com.azure:azure-core") {
    version {
      // want to test with earliest version supported, and not managed version used in agent
      strictly("1.14.0")
    }
  }
}
