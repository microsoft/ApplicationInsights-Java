plugins {
  id("ai.java-conventions")
  id("ai.sdk-version-file")
}

dependencies {
  // needed to access io.opentelemetry.instrumentation.api.aisdk.MicrometerUtil
  // TODO (heya) remove this when updating to upstream micrometer instrumentation
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry:opentelemetry-semconv")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  implementation("ch.qos.logback:logback-classic")
  implementation("ch.qos.logback.contrib:logback-json-classic")

  // not using gson because it has dependency on java.sql.*, which is not available in Java 9+ bootstrap class loader
  // only complaint so far about moshi is that it doesn"t give line numbers when there are json formatting errors
  implementation("com.squareup.moshi:moshi")

  implementation(project(":etw:java"))

  testCompileOnly("com.google.code.findbugs:jsr305")

  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.mockito:mockito-core")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.2")
}
