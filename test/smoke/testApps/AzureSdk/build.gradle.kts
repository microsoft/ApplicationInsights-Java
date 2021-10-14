plugins {
  id("ai.smoke-test-war")
}

// want to test with earliest version supported, and not managed version used in agent
// can't use enforcedPlatform since predates BOM
configurations.testRuntimeClasspath.resolutionStrategy.force("com.azure:azure-core:1.14.0")

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
}
