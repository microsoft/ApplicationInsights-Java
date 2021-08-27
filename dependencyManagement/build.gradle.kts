import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  `java-platform`

  id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

val otelVersion = "1.5.0"
rootProject.extra["otelVersion"] = otelVersion

// IMPORTANT when updating opentelemetry version, be sure to update bytebuddy version to match
val otelInstrumentationVersionAlpha = "1.5.0+ai.patches-alpha"

val DEPENDENCY_BOMS = listOf(
  "com.google.guava:guava-bom:30.1.1-jre",
  "io.opentelemetry:opentelemetry-bom:${otelVersion}",
  "io.opentelemetry:opentelemetry-bom-alpha:${otelVersion}-alpha",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${otelInstrumentationVersionAlpha}",
  "org.junit:junit-bom:5.7.2"
)

// TODO consolidate to just one json library
// TODO remove dependencies on apache commons

val DEPENDENCY_SETS = listOf(
  DependencySet(
    "com.google.auto.value",
    "1.8.1",
    listOf("auto-value", "auto-value-annotations")
  ),
  DependencySet(
    "com.google.errorprone",
    "2.7.1",
    listOf("error_prone_annotations", "error_prone_core")
  ),
  DependencySet(
    "net.bytebuddy",
    // When updating, also update buildSrc/build.gradle.kts
    "1.11.2",
    listOf("byte-buddy", "byte-buddy-agent", "byte-buddy-gradle-plugin")
  ),
  DependencySet(
    "org.mockito",
    "3.11.1",
    listOf("mockito-core", "mockito-junit-jupiter")
  ),
  DependencySet(
    "org.slf4j",
    "1.7.30",
    listOf("slf4j-api", "slf4j-simple", "log4j-over-slf4j", "jcl-over-slf4j", "jul-to-slf4j")
  ),
  DependencySet(
    "org.testcontainers",
    "1.15.3",
    listOf("testcontainers", "junit-jupiter")
  ),
  DependencySet(
    "com.squareup.moshi",
    "1.11.0", // 1.12.0 and above use okio 2.x which pulls in kotlin libs
    listOf("moshi", "moshi-adapters")
  ),
  DependencySet(
    "io.opentelemetry.javaagent",
    "${otelInstrumentationVersionAlpha}",
    listOf(
      "opentelemetry-javaagent-instrumentation-api",
      "opentelemetry-javaagent-bootstrap",
      "opentelemetry-javaagent-tooling",
      "opentelemetry-javaagent-extension-api")
  ),
  DependencySet(
    "com.microsoft.azure",
    "2.6.3", // need the latest version for Java 16+ support without having to use --illegal-access=permit
    listOf(
      "applicationinsights-core",
      "applicationinsights-logging-log4j1_2",
      "applicationinsights-logging-log4j2",
      "applicationinsights-logging-logback",
      "applicationinsights-web",
      "applicationinsights-web-auto",
      "applicationinsights-spring-boot-starter")
  )
)

val DEPENDENCIES = listOf(
  "ch.qos.logback:logback-classic:1.2.3",
  "ch.qos.logback.contrib:logback-json-classic:0.1.5",
  "com.google.auto.service:auto-service:1.0",
  "com.uber.nullaway:nullaway:0.9.1",
  "commons-codec:commons-codec:1.15",
  "commons-io:commons-io:2.7",
  "org.apache.commons:commons-lang3:3.7",
  "org.apache.commons:commons-text:1.9",
  "com.google.code.gson:gson:2.8.2",
  "com.azure:azure-core:1.18.0",
  "com.azure:azure-storage-blob:12.13.0",
  "com.github.oshi:oshi-core:5.8.0",
  "org.assertj:assertj-core:3.19.0",
  "org.awaitility:awaitility:4.1.0",
  "io.github.hakky54:logcaptor:2.5.0",
  "com.microsoft.jfr:jfr-streaming:1.2.0",
  "org.checkerframework:checker-qual:3.14.0"
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
    for (set in DEPENDENCY_SETS) {
      for (module in set.modules) {
        api("${set.group}:${module}:${set.version}")
        dependencyVersions[set.group] = set.version
      }
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
