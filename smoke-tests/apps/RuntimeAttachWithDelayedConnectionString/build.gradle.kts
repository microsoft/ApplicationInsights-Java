plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation(project(":agent:runtime-attach"))
  implementation(project(":classic-sdk:core"))

  implementation("org.springframework.boot:spring-boot-starter-web:2.2.0.RELEASE")
}
