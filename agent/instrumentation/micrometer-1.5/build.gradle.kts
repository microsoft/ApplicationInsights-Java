plugins {
  id("ai.java-conventions")
  id("ai.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.micrometer")
    module.set("micrometer-core")
    versions.set("[1.5.0,)")
    assertInverse.set(true)
  }
}

val otelInstrumentationAlphaVersion: String by project

dependencies {
  compileOnly("io.micrometer:micrometer-core:1.5.0")

  compileOnly("io.opentelemetry.instrumentation:opentelemetry-micrometer-1.5:$otelInstrumentationAlphaVersion")

  // TODO remove when start using io.opentelemetry.instrumentation.javaagent-instrumentation plugin
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")

  add("codegen", "io.opentelemetry.instrumentation:opentelemetry-micrometer-1.5:$otelInstrumentationAlphaVersion")
}
