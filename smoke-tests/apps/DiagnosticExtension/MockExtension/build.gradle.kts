plugins {
  java
}

dependencies {
  compileOnly(project(":agent:agent-profiler:agent-diagnostics-api"))
  compileOnly(project(":agent:agent-profiler:agent-alerting-api"))
}


java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

tasks.withType<JavaCompile>().configureEach {
  with(options) {
    release.set(8)
    compilerArgs.add("-Werror")
    // We need to support compiling to Java 8 even when using JDK 21 to build.
    // Suppress obsolete source/target warning added in JDK 21 while retaining -Werror for everything else.
    // This only disables the 'options' lint category (e.g., the obsolete source/target messages).
    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
      compilerArgs.add("-Xlint:-options")
    }
  }
}

tasks.jar {
  archiveFileName.set("extension.jar")
}
