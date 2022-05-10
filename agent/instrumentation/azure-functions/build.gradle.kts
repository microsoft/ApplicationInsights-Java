plugins {
  id("ai.java-conventions")
  id("ai.javaagent-instrumentation")

  id("io.opentelemetry.instrumentation.muzzle-generation") version "1.13.1-alpha"
  id("io.opentelemetry.instrumentation.muzzle-check") version "1.13.1-alpha"
}

muzzle {
  pass {
    coreJdk()
  }
}

val otelInstrumentationVersionAlpha: String by project

dependencies {
  // TODO remove when start using io.opentelemetry.instrumentation.javaagent-instrumentation plugin
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersionAlpha")
  add("codegen", "ch.qos.logback:logback-classic:1.2.3")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-annotation-support:$otelInstrumentationVersionAlpha")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationVersionAlpha")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersionAlpha")
}
