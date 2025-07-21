plugins {
  id("ai.java-conventions")
  id("ai.sdk-version-file")
}

dependencies {
  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  implementation("io.opentelemetry.contrib:opentelemetry-jfr-connection")
  implementation("com.azure:azure-storage-blob")

  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  implementation(project(":agent:agent-profiler:agent-alerting"))
  implementation(project(":agent:agent-gc-monitor:gc-monitor-api"))
  implementation(project(":agent:agent-gc-monitor:gc-monitor-core"))
  implementation(project(":agent:agent-profiler:agent-diagnostics-api"))
  implementation(project(":agent:agent-profiler:agent-diagnostics"))
  implementation(project(":etw:java"))

  implementation("com.azure:azure-monitor-opentelemetry-autoconfigure:1.2.0")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling-java9")
  compileOnly("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")

  implementation("commons-codec:commons-codec")
  implementation("org.apache.commons:commons-text")
  // TODO (trask) this is probably still needed for above apache commons projects
  implementation("org.slf4j:jcl-over-slf4j")

  implementation("ch.qos.logback:logback-classic")
  implementation("ch.qos.logback.contrib:logback-json-classic")

  implementation("com.azure:azure-core")
  implementation("com.azure:azure-identity") {
    exclude("org.ow2.asm", "asm")
  }

  compileOnly("io.opentelemetry:opentelemetry-api-incubator")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-extension-trace-propagators")

  implementation("com.github.oshi:oshi-core:6.8.2") {
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

  // TODO(trask): update tests, no need to use this anymore
  testImplementation("com.squareup.okio:okio:3.15.0")

  compileOnly(project(":agent:agent-bootstrap"))
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":agent:agent-bootstrap"))
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support")

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("com.azure:azure-core-test")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.awaitility:awaitility")
  testImplementation("org.mockito:mockito-core")

  testCompileOnly("com.google.code.findbugs:jsr305")
}

configurations.all {
  // waiting for azure-identity to be bumped in the Azure SDK BOM
  resolutionStrategy.force("com.azure:azure-identity:1.16.2")
}

configurations {
  "implementation" {
    exclude(group = "net.bytebuddy", module = "byte-buddy") // we use byte-buddy-dep
  }
}
