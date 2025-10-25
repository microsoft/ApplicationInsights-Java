plugins {
  java
}

dependencies {
  compileOnly("org.gradle.jfr.polyfill:jfr-polyfill:1.0.0")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

tasks.withType<JavaCompile>().configureEach {
  with(options) {
    release.set(11)
    compilerArgs.add("-Werror")
  }
}
