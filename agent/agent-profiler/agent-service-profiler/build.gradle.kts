plugins {
  id("ai.java-conventions")
}

dependencies {
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  implementation("com.microsoft.jfr:jfr-streaming")
  implementation("org.slf4j:slf4j-api")
  implementation("com.squareup.moshi:moshi")
  implementation("com.squareup.moshi:moshi-adapters")
  implementation("com.azure:azure-storage-blob")

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.mockito:mockito-core")
}
