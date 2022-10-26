plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  // this dependency is needed to make wildfly happy
  implementation("org.reactivestreams:reactive-streams:1.0.3")
}
