plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation(project(":classic-sdk:core"))
  implementation("org.apache.httpcomponents:httpclient:4.5.7")
}
