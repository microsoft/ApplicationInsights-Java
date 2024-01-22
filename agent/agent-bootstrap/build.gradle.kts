plugins {
  id("ai.java-conventions")
  id("ai.sdk-version-file")
}

dependencies {
  // needed to access io.opentelemetry.instrumentation.api.aisdk.MicrometerUtil
  // TODO (heya) remove this when updating to upstream micrometer instrumentation
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
<<<<<<< HEAD
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
=======
  compileOnly("io.opentelemetry:opentelemetry-semconv")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
>>>>>>> dependabot/gradle/otelInstrumentationAlphaVersion-2.0.0-alpha
}
