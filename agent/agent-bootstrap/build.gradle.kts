plugins {
  id("ai.java-conventions")
  id("ai.sdk-version-file")
}

dependencies {
  // needed to access io.opentelemetry.instrumentation.api.aisdk.MicrometerUtil
  // TODO (heya) remove this when updating to upstream micrometer instrumentation
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
}
