plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12")
}

tasks.withType<Jar> {
  manifest {
    attributes(
      "Implementation-Version" to "1.2.3"
    )
  }
}
