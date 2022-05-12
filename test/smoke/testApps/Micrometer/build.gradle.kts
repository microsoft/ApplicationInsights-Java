plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE")
  // TODO (trask) do we still need to support earlier micrometer versions?
  implementation("io.micrometer:micrometer-core:1.5.0")
}
