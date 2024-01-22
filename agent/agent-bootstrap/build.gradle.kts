plugins {
  id("ai.java-conventions")
  id("ai.sdk-version-file")
}

dependencies {
  // needed to access io.opentelemetry.instrumentation.api.aisdk.MicrometerUtil
  // TODO (heya) remove this when updating to upstream micrometer instrumentation
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry:opentelemetry-semconv")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
}
