import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  `java-platform`

  id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

val otelVersion = "1.18.0"
// IMPORTANT when updating opentelemetry instrumentation version, be sure to update bytebuddy version to match
val otelInstrumentationVersion = "1.18.0"
val otelInstrumentationAlphaVersion = "1.18.0-alpha"
val otelContribAlphaVersion = "1.17.0-alpha"

rootProject.extra["otelVersion"] = otelVersion
rootProject.extra["otelInstrumentationVersion"] = otelInstrumentationVersion
rootProject.extra["otelInstrumentationAlphaVersion"] = otelInstrumentationAlphaVersion
rootProject.extra["otelContribAlphaVersion"] = otelContribAlphaVersion

val DEPENDENCY_BOMS = listOf(
  "com.google.guava:guava-bom:31.1-jre",
  "io.opentelemetry:opentelemetry-bom:${otelVersion}",
  "io.opentelemetry:opentelemetry-bom-alpha:${otelVersion}-alpha",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:${otelInstrumentationVersion}",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${otelInstrumentationAlphaVersion}",
  "com.azure:azure-sdk-bom:1.2.6",
  "org.junit:junit-bom:5.8.2",
  "org.testcontainers:testcontainers-bom:1.17.3",
)

// TODO consolidate to just one json library
// TODO remove dependencies on apache commons

val CORE_DEPENDENCIES = listOf(
  "com.google.auto.service:auto-service:1.0.1",
  "com.google.auto.service:auto-service-annotations:1.0.1",
  "com.google.auto.value:auto-value:1.9",
  "com.google.auto.value:auto-value-annotations:1.9",
  "com.google.errorprone:error_prone_annotations:2.14.0",
  "com.google.errorprone:error_prone_core:2.14.0",
  // When updating, also update buildSrc/build.gradle.kts
  "net.bytebuddy:byte-buddy:1.12.10",
  "net.bytebuddy:byte-buddy-dep:1.12.10",
  "net.bytebuddy:byte-buddy-agent:1.12.10",
  "net.bytebuddy:byte-buddy-gradle-plugin:1.12.10",
  "org.openjdk.jmh:jmh-core:1.35",
  "org.openjdk.jmh:jmh-generator-bytecode:1.35",
  "org.mockito:mockito-core:4.8.0",
  "org.mockito:mockito-junit-jupiter:4.8.0",
  "org.mockito:mockito-inline:4.8.0",
  // moving to 2.0 is problematic because the SPI mechanism doesn't work in the bootstrap class
  // loader because while we add the agent jar to the bootstrap class loader via
  // Instrumentation.appendToBootstrapClassLoaderSearch(), there's nothing similar for resources
  // (which is a known problem in the java agent world) and so the META-INF/services resource is
  // not found
  "org.slf4j:slf4j-api:1.7.36",
  "org.slf4j:slf4j-simple:1.7.36",
  "org.slf4j:log4j-over-slf4j:1.7.36",
  "org.slf4j:jcl-over-slf4j:1.7.36",
  "org.slf4j:jul-to-slf4j:1.7.36",
  // 1.12.0 and above use okio 2.x which pulls in kotlin libs
  "com.squareup.moshi:moshi:1.11.0",
  "com.squareup.moshi:moshi-adapters:1.11.0",
  "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:otelInstrumentationAlphaVersion",
  "io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:otelInstrumentationAlphaVersion",
  "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:otelInstrumentationAlphaVersion",
  "com.microsoft.azure:applicationinsights-core:2.6.4",
  "com.microsoft.azure:applicationinsights-logging-log4j1_2:2.6.4",
  "com.microsoft.azure:applicationinsights-logging-log4j2:2.6.4",
  "com.microsoft.azure:applicationinsights-logging-logback:2.6.4",
  "com.microsoft.azure:applicationinsights-web:2.6.4",
  "com.microsoft.azure:applicationinsights-web-auto:2.6.4",
  "com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4",
)

val DEPENDENCIES = listOf(
  "ch.qos.logback:logback-classic:1.2.11",
  "ch.qos.logback.contrib:logback-json-classic:0.1.5",
  "com.google.auto.service:auto-service:1.0.1",
  "com.uber.nullaway:nullaway:0.9.8",
  "commons-codec:commons-codec:1.15",
  "org.apache.commons:commons-text:1.9",
  "com.google.code.gson:gson:2.8.2",
  "com.azure:azure-core-test:1.9.1",
  "com.github.oshi:oshi-core:6.2.0",
  "org.assertj:assertj-core:3.23.1",
  "org.awaitility:awaitility:4.2.0",
  "io.github.hakky54:logcaptor:2.7.9",
  "com.microsoft.jfr:jfr-streaming:1.2.0",
  "com.google.code.findbugs:jsr305:3.0.2",
  "com.github.spotbugs:spotbugs-annotations:4.7.1"
)

javaPlatform {
  allowDependencies()
}

dependencies {
  for (bom in DEPENDENCY_BOMS) {
    api(enforcedPlatform(bom))
    val split = bom.split(':')
    dependencyVersions[split[0]] = split[2]
  }
  constraints {
    for (dependency in CORE_DEPENDENCIES) {
      api(dependency)
      val split = dependency.split(':')
      dependencyVersions[split[0]] = split[2]
    }
    for (dependency in DEPENDENCIES) {
      api(dependency)
      val split = dependency.split(':')
      dependencyVersions[split[0]] = split[2]
    }
  }
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isGuava = version.endsWith("-jre")
  val isStable = stableKeyword || regex.matches(version) || isGuava
  return isStable.not()
}

tasks {
  named<DependencyUpdatesTask>("dependencyUpdates") {
    revision = "release"
    checkConstraints = true

    rejectVersionIf {
      isNonStable(candidate.version)
    }
  }
}
