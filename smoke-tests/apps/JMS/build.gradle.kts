plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE")
  implementation("org.springframework.boot:spring-boot-starter-activemq:2.1.7.RELEASE")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
}
