import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  `java-platform`

  id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

val otelVersion = "1.16.0"
// IMPORTANT when updating opentelemetry instrumentation version, be sure to update bytebuddy version to match
val otelInstrumentationVersion = "1.16.0"
val otelInstrumentationAlphaVersion = "1.16.0-alpha"
val otelContribAlphaVersion = "1.16.0-alpha"

rootProject.extra["otelVersion"] = otelVersion
rootProject.extra["otelInstrumentationVersion"] = otelInstrumentationVersion
rootProject.extra["otelInstrumentationAlphaVersion"] = otelInstrumentationAlphaVersion
rootProject.extra["otelContribAlphaVersion"] = otelContribAlphaVersion

val DEPENDENCY_BOMS = listOf(
  "com.google.guava:guava-bom:31.1-jre",
  "io.opentelemetry:opentelemetry-bom:${otelVersion}",
  "io.opentelemetry:opentelemetry-bom-alpha:${otelVersion}-alpha",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${otelInstrumentationAlphaVersion}",
  "com.azure:azure-sdk-bom:1.2.3",
  "org.junit:junit-bom:5.8.2"
)

// TODO consolidate to just one json library
// TODO remove dependencies on apache commons

val DEPENDENCY_SETS = listOf(
  DependencySet(
    "com.google.auto.value",
    "1.9",
    listOf("auto-value", "auto-value-annotations")
  ),
  DependencySet(
    "com.google.errorprone",
    "2.14.0",
    listOf("error_prone_annotations", "error_prone_core")
  ),
  DependencySet(
    "net.bytebuddy",
    // When updating, also update buildSrc/build.gradle.kts
    "1.12.10",
    listOf("byte-buddy", "byte-buddy-dep", "byte-buddy-agent", "byte-buddy-gradle-plugin")
  ),
  DependencySet(
    "org.mockito",
    "4.6.1",
    listOf("mockito-core", "mockito-junit-jupiter")
  ),
  DependencySet(
    "org.slf4j",
    "1.7.36",
    listOf("slf4j-api", "slf4j-simple", "log4j-over-slf4j", "jcl-over-slf4j", "jul-to-slf4j")
  ),
  DependencySet(
    "org.testcontainers",
    "1.17.3",
    listOf("testcontainers", "junit-jupiter")
  ),
  DependencySet(
    "com.squareup.moshi",
    "1.11.0", // 1.12.0 and above use okio 2.x which pulls in kotlin libs
    listOf("moshi", "moshi-adapters")
  ),
  DependencySet(
    "io.opentelemetry.javaagent",
    "${otelInstrumentationAlphaVersion}",
    listOf(
      "opentelemetry-javaagent-extension-api",
      "opentelemetry-javaagent-bootstrap",
      "opentelemetry-javaagent-tooling",
      "opentelemetry-javaagent-extension-api")
  ),
  DependencySet(
    "com.microsoft.azure",
    "2.6.4", // need the latest version for Java 16+ support without having to use --illegal-access=permit
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
