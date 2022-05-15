plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
  implementation("org.apache.httpcomponents:httpasyncclient:4.1.4")
  implementation("commons-httpclient:commons-httpclient:3.1")
  implementation("com.squareup.okhttp3:okhttp:3.12.1")
  implementation("com.squareup.okhttp:okhttp:2.7.5")
  implementation("org.springframework:spring-webflux:5.2.3.RELEASE") // for testing netty client
  implementation("io.projectreactor.netty:reactor-netty:0.9.4.RELEASE") // needed for above
  // this dependency is needed to make wildfly happy
  implementation("com.fasterxml.jackson.core:jackson-databind:2.9.4")
}
