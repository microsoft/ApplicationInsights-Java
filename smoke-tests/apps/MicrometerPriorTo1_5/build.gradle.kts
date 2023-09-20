plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12")
  implementation("io.micrometer:micrometer-core:1.4.1")
}
