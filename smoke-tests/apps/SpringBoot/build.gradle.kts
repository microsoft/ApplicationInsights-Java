plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.7.18") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }

  implementation("org.apache.httpcomponents:httpclient:4.5.13")
}
