plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation(project(":agent:agent-gc-monitor:gc-monitor-api"))
  implementation("org.slf4j:slf4j-api")
}
