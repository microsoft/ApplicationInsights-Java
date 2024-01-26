plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  // this dependency is needed to make wildfly happy
  implementation("org.reactivestreams:reactive-streams:1.0.3")

  implementation("org.weakref:jmxutils:1.22")

  // spring modules
  smokeTestImplementation("org.mock-server:mockserver-netty:5.15.0:shaded")
  smokeTestImplementation("org.awaitility:awaitility:4.2.0")
  smokeTestImplementation("io.opentelemetry.proto:opentelemetry-proto:0.14.0-alpha")
}
