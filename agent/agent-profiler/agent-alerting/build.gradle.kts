plugins {
  id("ai.java-conventions")
}

dependencies {
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  implementation("org.slf4j:slf4j-api")

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
}
