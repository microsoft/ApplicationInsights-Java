plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("com.microsoft.azure:applicationinsights-core")
  implementation("org.apache.httpcomponents:httpclient:4.5.7")
}
