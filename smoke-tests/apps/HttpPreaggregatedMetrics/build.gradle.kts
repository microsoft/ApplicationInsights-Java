plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.apache.httpcomponents:httpasyncclient:4.1.4")
  // this dependency is needed to make wildfly happy
  implementation("com.fasterxml.jackson.core:jackson-databind:2.9.4")
}
