plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")

  implementation("org.springframework.boot:spring-boot-starter-web") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }

  // this dependency is needed to make wildfly happy
  implementation("org.reactivestreams:reactive-streams:1.0.3")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
}
