plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation(project(":classic-sdk:web"))
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  // this dependency is needed to make wildfly happy
  implementation("org.reactivestreams:reactive-streams:1.0.3")
}
