plugins {
  `java`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

dependencies {
  compileOnly("org.gradle.jfr.polyfill:jfr-polyfill:1.0.0")
}
