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
  implementation("org.apache.commons:commons-lang3:3.17.0")

  implementation("com.google.code.gson:gson")
  implementation("org.apache.httpcomponents:httpclient:4.5.14")

  implementation("org.eclipse.jetty:jetty-servlet:10.0.24")

  implementation("org.mock-server:mockserver-netty:5.15.0:shaded")
  implementation("io.opentelemetry.proto:opentelemetry-proto:1.5.0-alpha")

  // this is exposed in SmokeTestExtension API
  api("org.testcontainers:testcontainers:1.20.6")

  implementation("org.awaitility:awaitility:4.3.0")

  implementation("ch.qos.logback:logback-classic")

  implementation("org.assertj:assertj-core")
}
