plugins {
  id("ai.java-conventions")
  id("ai.sdk-version-file")
}

dependencies {
  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  implementation("com.microsoft.jfr:jfr-streaming")
  implementation("com.azure:azure-storage-blob")
  implementation("com.squareup.moshi:moshi")
  implementation("com.squareup.moshi:moshi-adapters")

  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  implementation(project(":agent:agent-profiler:agent-alerting"))
  implementation(project(":agent:agent-gc-monitor:gc-monitor-api"))
  implementation(project(":agent:agent-gc-monitor:gc-monitor-core"))
  implementation(project(":agent:agent-profiler:agent-diagnostics-api"))
  implementation(project(":agent:azure-monitor-exporter")) {
    exclude("org.ow2.asm", "asm")
  }

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")

  implementation("commons-codec:commons-codec")
  implementation("org.apache.commons:commons-text")
  // TODO (trask) this is probably still needed for above apache commons projects
  implementation("org.slf4j:jcl-over-slf4j")

  // these are present in the bootstrap class loader
  compileOnly("ch.qos.logback:logback-classic")
  compileOnly("ch.qos.logback.contrib:logback-json-classic")

  implementation("com.azure:azure-core")
  implementation("com.azure:azure-identity") {
    exclude("org.ow2.asm", "asm")
  }

  //  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-tracing-incubator")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-extension-trace-propagators")

  implementation("com.github.oshi:oshi-core:6.4.2") {
    exclude("org.slf4j", "slf4j-api")
  }

  compileOnly("org.slf4j:slf4j-api")

  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-metrics")
  compileOnly("io.opentelemetry:opentelemetry-sdk-logs")

  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-metrics")
  testImplementation("io.opentelemetry:opentelemetry-sdk-logs")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-logs-testing")

  // TODO(trask): update tests, no need to use this anymore
  testImplementation("com.squareup.okio:okio:3.3.0")

  compileOnly(project(":agent:agent-bootstrap"))
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":agent:agent-bootstrap"))
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support")

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("com.azure:azure-core-test")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.awaitility:awaitility")
  testImplementation("org.mockito:mockito-core")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.2")
  testImplementation("io.github.hakky54:logcaptor")
}

configurations {
  all {
    // excluding unused dependencies for size (~1.8mb)
    exclude("com.fasterxml.jackson.dataformat", "jackson-dataformat-xml")
    exclude("com.fasterxml.woodstox", "woodstox-core")
  }
}
