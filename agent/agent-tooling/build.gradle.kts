plugins {
  id("ai.java-conventions")
  id("ai.sdk-version-file")
  id("com.github.johnrengelman.shadow")
}

// Adding this step to copy playback test results from session-records to build/classes/java/test. Azure core testing framework follows this directory structure.
sourceSets {
  test {
    output.setResourcesDir("build/classes/java/test")
  }
}

dependencies {
  implementation(project(":agent:agent-profiler:agent-service-profiler"))
  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  implementation(project(":agent:agent-profiler:agent-alerting"))
  implementation(project(":agent:agent-gc-monitor:gc-monitor-api"))
  implementation(project(":agent:agent-gc-monitor:gc-monitor-core"))

  implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  implementation("net.bytebuddy:byte-buddy")

  implementation("commons-codec:commons-codec")
  implementation("org.apache.commons:commons-lang3")
  implementation("commons-io:commons-io")
  implementation("org.apache.commons:commons-text")
  // TODO (trask) this is probably still needed for above apache commons projects
  implementation("org.slf4j:jcl-over-slf4j")

  implementation("org.checkerframework:checker-qual")
  implementation("com.google.code.findbugs:annotations:3.0.1")

  implementation("ch.qos.logback:logback-classic")
  implementation("ch.qos.logback.contrib:logback-json-classic")

  implementation(project(":agent:agent-profiler:agent-profiler-api"))

  implementation("com.azure:azure-monitor-opentelemetry-exporter:1.0.0-beta.4")
  implementation("com.azure:azure-core")
  implementation("com.azure:azure-identity:1.2.4") {
    // "This dependency can be excluded if IntelliJ Credential is not being used for authentication
    //  via `IntelliJCredential` or `DefaultAzureCredential`"
    // NOTE this exclusion saves 6.5 mb !!!!
    exclude("org.linguafranca.pwdb", "KeePassJava2")
  }

  implementation("io.opentelemetry:opentelemetry-sdk-extension-tracing-incubator")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  implementation("com.github.oshi:oshi-core")
  implementation("org.slf4j:slf4j-api")

  implementation("io.opentelemetry:opentelemetry-api")
  implementation("org.jctools:jctools-core:3.3.0")

  compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  // TODO(trask): update tests, no need to use this anymore
  testImplementation("com.squareup.okio:okio:2.8.0")

  compileOnly(project(":agent:agent-bootstrap"))
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-annotation-support")

  testImplementation(project(":agent:agent-bootstrap"))
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-annotation-support")

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("com.azure:azure-core-test:1.7.0")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.awaitility:awaitility")
  testImplementation("org.mockito:mockito-core")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:1.1.0")
  testImplementation("io.github.hakky54:logcaptor")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("com.microsoft.jfr:jfr-streaming")
  testImplementation("com.azure:azure-storage-blob")
}


