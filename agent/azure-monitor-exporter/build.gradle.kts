plugins {
  id("ai.java-conventions")
}

// Adding this step to copy playback test results from session-records to build/classes/java/test. Azure core testing framework follows this directory structure.
sourceSets {
  test {
    output.setResourcesDir("build/classes/java/test")
  }
}

dependencies {
  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  implementation("com.azure:azure-core")
  implementation("com.azure:azure-identity")

  // CVE-2023-1370 - https://github.com/advisories/GHSA-493p-pfq6-5258
  // Transitive dependency: json-smart -> com.microsoft.azure:msal4j:1.13.5 ->  com.azure:azure-identity
  // -> azure-monitor-exporter
  // upstream fix: https://github.com/AzureAD/microsoft-authentication-library-for-java/pull/612
  implementation("net.minidev:json-smart:2.4.10")

  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-metrics")
  compileOnly("io.opentelemetry:opentelemetry-sdk-logs")

  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-metrics")
  testImplementation("io.opentelemetry:opentelemetry-sdk-logs")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("com.azure:azure-core-test")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.awaitility:awaitility")
  testImplementation("org.mockito:mockito-core")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.2")
  testImplementation("io.github.hakky54:logcaptor")

  testImplementation("com.azure:azure-data-appconfiguration:1.4.4")
  testImplementation("com.azure:azure-messaging-eventhubs:5.15.3")
  testImplementation("com.azure:azure-messaging-eventhubs-checkpointstore-blob:1.16.4")

  testImplementation("com.squareup.okio:okio:3.3.0")

  testCompileOnly("com.google.code.findbugs:jsr305")
  testCompileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

configurations {
  all {
    // excluding unused dependencies for size (~1.8mb)
    exclude("com.fasterxml.jackson.dataformat", "jackson-dataformat-xml")
    exclude("com.fasterxml.woodstox", "woodstox-core")
  }
}
