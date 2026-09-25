plugins {
  java
}

dependencies {
  compileOnly(platform(project(":dependencyManagement")))
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk")
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
    compilerArgs.add("-Xlint:-options")
  }
}

tasks.jar {
  archiveFileName.set("extension.jar")
}
