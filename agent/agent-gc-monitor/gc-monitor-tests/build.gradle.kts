plugins {
  id("ai.java-conventions")
}

dependencies {
  testImplementation(project(":agent:agent-gc-monitor:gc-monitor-core"))
  testImplementation(project(":agent:agent-gc-monitor:gc-monitor-api"))

  testImplementation(platform("org.junit:junit-bom"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
}

tasks {
  withType<JavaCompile>().configureEach {
    options.release.set(11)
  }
}
