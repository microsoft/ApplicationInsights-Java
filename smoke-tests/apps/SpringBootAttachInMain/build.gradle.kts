plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation(project(":agent:runtime-attach"))

  implementation("org.springframework.boot:spring-boot-starter-web")
}
