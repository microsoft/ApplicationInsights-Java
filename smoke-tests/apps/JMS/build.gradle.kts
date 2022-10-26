plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-activemq")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
}
