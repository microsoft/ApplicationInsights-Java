plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:1.3.8.RELEASE"))

  implementation("org.springframework.boot:spring-boot-starter-web") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
}
