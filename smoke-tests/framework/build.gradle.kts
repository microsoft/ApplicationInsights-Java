plugins {
  id("ai.java-conventions")
}

tasks {
  named("spotbugsMain") {
    enabled = false
  }
}

dependencies {
  implementation("com.google.guava:guava")
  implementation("org.junit.jupiter:junit-jupiter")
  implementation("org.apache.commons:commons-lang3:3.20.0")

  implementation("com.google.code.gson:gson")
  implementation("org.apache.httpcomponents:httpclient:4.5.14")

  implementation("org.eclipse.jetty:jetty-servlet:10.0.26")

  implementation("org.mock-server:mockserver-netty:5.15.0:shaded")
  implementation("io.opentelemetry.proto:opentelemetry-proto:1.8.0-alpha")

  // this is exposed in SmokeTestExtension API
  api("org.testcontainers:testcontainers:2.0.2")

  implementation("org.awaitility:awaitility:4.3.0")

  implementation("ch.qos.logback:logback-classic")

  implementation("org.assertj:assertj-core")

  implementation("com.azure:azure-json:1.5.0")
  implementation("com.azure:azure-monitor-opentelemetry-autoconfigure:1.4.0")
}
