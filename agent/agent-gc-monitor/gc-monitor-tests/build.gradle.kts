plugins {
  id("ai.java-conventions")
}

dependencies {
  testImplementation(project(":agent:agent-gc-monitor:gc-monitor-core"))
  testImplementation(project(":agent:agent-gc-monitor:gc-monitor-api"))

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
}
