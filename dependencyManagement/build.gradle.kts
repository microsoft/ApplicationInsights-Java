import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  `java-platform`

  id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

val otelVersion = "1.4.1"
rootProject.extra["otelVersion"] = otelVersion

val otelInstrumentationVersion = "1.3.1+ai.patch.1"

val DEPENDENCY_BOMS = listOf(
  "io.opentelemetry:opentelemetry-bom:${otelVersion}",
  "io.opentelemetry:opentelemetry-bom-alpha:${otelVersion}-alpha",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${otelInstrumentationVersion}-alpha",
  "org.junit:junit-bom:5.7.2"
)

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
    "io.opentelemetry.javaagent",
    "${otelInstrumentationVersion}-alpha",
    listOf(
      "opentelemetry-javaagent-instrumentation-api",
      "opentelemetry-javaagent-bootstrap",
      "opentelemetry-javaagent-tooling",
      "opentelemetry-javaagent-extension-api")
  )
)

val DEPENDENCIES = listOf(
  "ch.qos.logback:logback-classic:1.2.3",
  "ch.qos.logback.contrib:logback-json-classic:0.1.5",
  "com.google.auto.service:auto-service:1.0",
  "com.uber.nullaway:nullaway:0.9.1",
  "commons-codec:commons-codec:1.15",
  "org.apache.commons:commons-lang3:3.7",
  "com.google.code.gson:gson:2.8.2",
  "com.azure:azure-core:1.17.0",
  "com.azure:azure-storage-blob:12.12.0",
  "com.squareup.moshi:moshi:1.11.0", // 1.12.0 and above use okio 2.x which pulls in kotlin libs
  "com.squareup.moshi:moshi-adapters:1.11.0",
  "com.github.oshi:oshi-core:5.6.0",
  "org.assertj:assertj-core:3.19.0",
  "io.github.hakky54:logcaptor:2.5.0",
  "com.microsoft.jfr:jfr-streaming:1.2.0",
  "org.checkerframework:checker-qual:3.14.0",
  "com.microsoft.azure:applicationinsights-core:2.6.3",
  "com.microsoft.azure:applicationinsights-web:2.6.3",
  "com.microsoft.azure:applicationinsights-web-auto:2.6.3",
  "com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.3",
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
