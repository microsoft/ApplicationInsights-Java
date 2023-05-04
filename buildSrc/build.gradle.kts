plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  // When updating, update below in dependencies too
  id("com.diffplug.spotless") version "6.18.0"
}

spotless {
  java {
    googleJavaFormat()
    licenseHeaderFile(rootProject.file("../buildscripts/spotless.license.java"), "(package|import|public)")
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
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.18.0")
  implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:5.0.14")
  implementation("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
  implementation("com.gradle.enterprise:com.gradle.enterprise.gradle.plugin:3.13.1")

  implementation("org.owasp:dependency-check-gradle:8.2.1")

  implementation("io.opentelemetry.instrumentation:gradle-plugins:1.25.1-alpha")

  implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.1.0")
  implementation("net.ltgt.gradle:gradle-nullaway-plugin:1.5.0")

  implementation("gradle.plugin.io.morethan.jmhreport:gradle-jmh-report:0.9.0")
  implementation("me.champeau.jmh:jmh-gradle-plugin:0.7.1")

  implementation("org.springframework.boot:spring-boot-gradle-plugin:2.2.0.RELEASE")
}
