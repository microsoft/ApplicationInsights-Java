plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  testImplementation(project(":smoke-tests:framework"))
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12")
}
