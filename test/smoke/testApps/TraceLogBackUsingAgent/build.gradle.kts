plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web-auto")

  implementation("ch.qos.logback:logback-classic:1.2.3")
}
