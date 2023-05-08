plugins {
  java
}

dependencies {
  compileOnly(project(":agent:agent-profiler:agent-diagnostics-api"))
  compileOnly(project(":agent:agent-profiler:agent-alerting-api"))
}


java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

tasks.withType<JavaCompile>().configureEach {
  with(options) {
    release.set(8)
    compilerArgs.add("-Werror")
  }
}

tasks.jar {
  archiveFileName.set("extension.jar")
}
