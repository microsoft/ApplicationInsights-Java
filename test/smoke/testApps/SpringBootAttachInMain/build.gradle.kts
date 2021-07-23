plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation(project(":agent:agent"))

  implementation("org.springframework.boot:spring-boot-starter-web:2.2.0.RELEASE")
  implementation("net.bytebuddy:byte-buddy-agent:1.11.0")
}
