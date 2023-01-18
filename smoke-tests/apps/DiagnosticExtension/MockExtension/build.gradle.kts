plugins {
  `java`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

dependencies {
  compileOnly(project(":agent:agent-profiler:agent-diagnostics-api"))
  compileOnly(project(":agent:agent-profiler:agent-alerting-api"))
}

tasks.jar {
  archiveFileName.set("extension.jar")
}
