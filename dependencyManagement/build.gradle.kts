import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  `java-platform`

  id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

val otelVersion = "1.25.0"
val otelInstrumentationAlphaVersion = "1.25.1-alpha"
val otelInstrumentationVersion = "1.25.1"
val otelContribAlphaVersion = "1.18.0-alpha"

rootProject.extra["otelVersion"] = otelVersion
rootProject.extra["otelInstrumentationVersion"] = otelInstrumentationVersion
rootProject.extra["otelInstrumentationAlphaVersion"] = otelInstrumentationAlphaVersion
rootProject.extra["otelContribAlphaVersion"] = otelContribAlphaVersion

val DEPENDENCY_BOMS = listOf(
  "com.fasterxml.jackson:jackson-bom:2.14.2",
  "com.google.guava:guava-bom:31.1-jre",
  "io.opentelemetry:opentelemetry-bom:${otelVersion}",
  "io.opentelemetry:opentelemetry-bom-alpha:${otelVersion}-alpha",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:${otelInstrumentationVersion}",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${otelInstrumentationAlphaVersion}",
  "com.azure:azure-sdk-bom:1.2.12",
  "io.netty:netty-bom:4.1.92.Final",
  "org.junit:junit-bom:5.9.3",
  "org.testcontainers:testcontainers-bom:1.18.0",
)

val autoServiceVersion = "1.0.1"
val autoValueVersion = "1.10.1"
val errorProneVersion = "2.18.0"
val byteBuddyVersion = "1.12.18"
val jmhVersion = "1.36"
val mockitoVersion = "4.11.0"
// moving to 2.0 is problematic because the SPI mechanism in 2.0 doesn't work in the
// bootstrap class loader because, while we add the agent jar to the bootstrap class loader
// via Instrumentation.appendToBootstrapClassLoaderSearch(), there's nothing similar for
// resources (which is a known problem in the java agent world), and so the META-INF/services
// resource is not found
val slf4jVersion = "1.7.36"
// 1.12.0 and above use okio 2.x which pulls in kotlin libs
val moshiVersion = "1.11.0"

val CORE_DEPENDENCIES = listOf(
  "com.google.auto.service:auto-service:${autoServiceVersion}",
  "com.google.auto.service:auto-service-annotations:${autoServiceVersion}",
  "com.google.auto.value:auto-value:${autoValueVersion}",
  "com.google.auto.value:auto-value-annotations:${autoValueVersion}",
  "com.google.errorprone:error_prone_annotations:${errorProneVersion}",
  "com.google.errorprone:error_prone_core:${errorProneVersion}",
  "org.openjdk.jmh:jmh-core:${jmhVersion}",
  "org.openjdk.jmh:jmh-generator-bytecode:${jmhVersion}",
  "org.mockito:mockito-core:${mockitoVersion}",
  "org.mockito:mockito-junit-jupiter:${mockitoVersion}",
  "org.mockito:mockito-inline:${mockitoVersion}",
  "org.slf4j:slf4j-api:${slf4jVersion}",
  "org.slf4j:log4j-over-slf4j:${slf4jVersion}",
  "org.slf4j:jcl-over-slf4j:${slf4jVersion}",
  "org.slf4j:jul-to-slf4j:${slf4jVersion}",
  "com.squareup.moshi:moshi:${moshiVersion}",
  "com.squareup.moshi:moshi-adapters:${moshiVersion}",
  "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${otelInstrumentationAlphaVersion}",
  "io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${otelInstrumentationAlphaVersion}",
  "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${otelInstrumentationAlphaVersion}",
  // temporarily overriding transitive dependency from azure-core until next azure-bom release
  // which targets at least reactor-netty-http:1.1.1
  "io.projectreactor.netty:reactor-netty-http:1.1.6",
  // CVE-2023-1370 - https://github.com/advisories/GHSA-493p-pfq6-5258
  // Transitive dependency: json-smart -> com.microsoft.azure:msal4j:1.13.5 ->  com.azure:azure-identity
  // -> azure-monitor-exporter
  // upstream fix: https://github.com/AzureAD/microsoft-authentication-library-for-java/pull/612
  "net.minidev:json-smart:2.4.10"
)

val DEPENDENCIES = listOf(
  "ch.qos.logback:logback-classic:1.2.12",
  "ch.qos.logback.contrib:logback-json-classic:0.1.5",
  "com.uber.nullaway:nullaway:0.10.10",
  "commons-codec:commons-codec:1.15",
  "org.apache.commons:commons-text:1.10.0",
  "com.google.code.gson:gson:2.10.1",
  "com.azure:azure-core-test:1.16.2", // this is not included in azure-sdk-bom
  "org.assertj:assertj-core:3.24.2",
  "org.awaitility:awaitility:4.2.0",
  "io.github.hakky54:logcaptor:2.9.0",
  "com.microsoft.jfr:jfr-streaming:1.2.0",
  "com.google.code.findbugs:jsr305:3.0.2",
  "com.github.spotbugs:spotbugs-annotations:4.7.3"
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
