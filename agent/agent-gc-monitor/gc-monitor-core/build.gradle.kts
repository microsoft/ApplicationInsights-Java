plugins {
  id("ai.java-conventions")
}

dependencies {
  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")

  implementation(project(":agent:agent-gc-monitor:gc-monitor-api"))
  implementation("org.slf4j:slf4j-api")
}
