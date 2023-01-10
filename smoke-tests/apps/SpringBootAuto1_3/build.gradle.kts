plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:1.3.8.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
}
