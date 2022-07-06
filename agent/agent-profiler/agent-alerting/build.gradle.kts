plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  implementation("org.slf4j:slf4j-api")

  implementation("com.google.guava:guava")
  implementation("io.opentelemetry:opentelemetry-sdk-trace")

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
}
