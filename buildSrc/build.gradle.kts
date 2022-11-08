plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  // When updating, update below in dependencies too
  id("com.diffplug.spotless") version "6.11.0"
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
  gradlePluginPortal()
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

dependencies {
  implementation(gradleApi())

  implementation("org.eclipse.aether:aether-connector-basic:1.1.0")
  implementation("org.eclipse.aether:aether-transport-http:1.1.0")
  implementation("org.apache.maven:maven-aether-provider:3.3.9")

  // When updating, update above in plugins too
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.11.0")
  implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:5.0.13")
  implementation("com.google.guava:guava:31.1-jre")
  implementation("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
  implementation("org.ow2.asm:asm:9.4")
  implementation("org.ow2.asm:asm-tree:9.4")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
  implementation("org.gradle:test-retry-gradle-plugin:1.4.1")

  implementation("org.owasp:dependency-check-gradle:7.3.0")

  implementation("io.opentelemetry.instrumentation:gradle-plugins:1.19.2-alpha")

  // we need to use byte buddy variant that does not shade asm
  implementation("net.bytebuddy:byte-buddy-gradle-plugin:1.12.18") {
    exclude(group = "net.bytebuddy", module = "byte-buddy")
  }
  implementation("net.bytebuddy:byte-buddy-dep:1.12.18")

  implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.0.1")
  implementation("net.ltgt.gradle:gradle-nullaway-plugin:1.5.0")

  implementation("gradle.plugin.io.morethan.jmhreport:gradle-jmh-report:0.9.0")
  implementation("me.champeau.jmh:jmh-gradle-plugin:0.6.8")

  implementation("org.springframework.boot:spring-boot-gradle-plugin:2.2.0.RELEASE")
}
