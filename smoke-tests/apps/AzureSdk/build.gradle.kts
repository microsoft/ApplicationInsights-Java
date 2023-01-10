plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  // want to test with one of the earliest version supported, and not managed version used in agent
  implementation(enforcedPlatform("com.azure:azure-sdk-bom:1.2.8"))
  implementation("com.azure:azure-core")
}
