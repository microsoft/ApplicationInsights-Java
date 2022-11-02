plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  implementation("com.azure:azure-core")
  implementation("com.squareup.moshi:moshi")
  implementation("org.slf4j:slf4j-api")
}

configurations {
  all {
    // excluding unused dependency for size (~1.8mb)
    exclude("com.fasterxml.jackson.dataformat", "jackson-dataformat-xml")
  }
}
