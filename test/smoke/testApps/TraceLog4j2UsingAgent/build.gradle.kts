plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.apache.logging.log4j:log4j-api:2.11.0")
  implementation("org.apache.logging.log4j:log4j-core:2.11.0")
  implementation("com.microsoft.azure:applicationinsights-web-auto")
}
