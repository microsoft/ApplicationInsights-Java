plugins {
  id("ai.java-conventions")
  id("ai.sdk-version-file")
}

val otelInstrumentationAlphaVersion = "1.13.0+ai.patches-alpha"

dependencies {
  implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap") {
    exclude("org.slf4j", "slf4j-simple")
  }
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-annotation-support")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-appender-api-internal")
  implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-instrumentation-api")
  implementation("ch.qos.logback:logback-classic")
  implementation("ch.qos.logback.contrib:logback-json-classic")

  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-executors-bootstrap:$otelInstrumentationAlphaVersion")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-common-bootstrap:$otelInstrumentationAlphaVersion")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-kafka-clients-0.11-bootstrap:$otelInstrumentationAlphaVersion")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common-bootstrap:$otelInstrumentationAlphaVersion")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-undertow-1.4-bootstrap:$otelInstrumentationAlphaVersion")

  // not using gson because it has dependency on java.sql.*, which is not available in Java 9+ bootstrap class loader
  // only complaint so far about moshi is that it doesn"t give line numbers when there are json formatting errors
  implementation("com.squareup.moshi:moshi")

  implementation(project(":etw:java"))

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.mockito:mockito-core")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:1.1.0")
}
