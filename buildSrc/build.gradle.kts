plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  // When updating, update below in dependencies too
  id("com.diffplug.spotless") version "7.2.1"
}

spotless {
  java {
    googleJavaFormat()
    licenseHeaderFile(
      rootProject.file("../buildscripts/spotless.license.java"),
      "(package|import|public)"
    )
    target("src/**/*.java")
  }
}

repositories {
  mavenCentral()
  mavenLocal()
  gradlePluginPortal()
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

dependencies {
  implementation(gradleApi())

  // When updating, update above in plugins too
  implementation("com.diffplug.spotless:spotless-plugin-gradle:7.2.1")
  implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.4.1")
  implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.9")

  implementation("org.owasp:dependency-check-gradle:12.1.3")

  implementation("io.opentelemetry.instrumentation:gradle-plugins:2.19.0-alpha")

  implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.3.0")
  implementation("net.ltgt.gradle:gradle-nullaway-plugin:2.3.0")

  implementation("gradle.plugin.io.morethan.jmhreport:gradle-jmh-report:0.9.6")
  implementation("me.champeau.jmh:jmh-gradle-plugin:0.7.3")

  // earlier versions aren't compatible with Gradle 8.1.1
  implementation("org.springframework.boot:spring-boot-gradle-plugin:2.5.12")
}
