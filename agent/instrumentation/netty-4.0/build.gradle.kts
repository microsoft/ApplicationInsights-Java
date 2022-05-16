plugins {
  id("ai.java-conventions")
  id("ai.javaagent-instrumentation")

  id("io.opentelemetry.instrumentation.muzzle-generation") version "1.13.1-alpha"
  id("io.opentelemetry.instrumentation.muzzle-check") version "1.13.1-alpha"
}

val otelInstrumentationVersionAlpha: String by project

dependencies {
  compileOnly("io.netty:netty-codec-http:4.0.0.Final")
  compileOnly("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.0:$otelInstrumentationVersionAlpha")

  add("codegen", "io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.0:$otelInstrumentationVersionAlpha")

  // TODO remove when start using io.opentelemetry.instrumentation.javaagent-instrumentation plugin
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersionAlpha")
  add("codegen", "ch.qos.logback:logback-classic:1.2.3")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-annotation-support:$otelInstrumentationVersionAlpha")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationVersionAlpha")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersionAlpha")
}
