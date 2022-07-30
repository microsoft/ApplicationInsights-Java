plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE")
  implementation("io.micrometer:micrometer-core:1.4.1")
}
