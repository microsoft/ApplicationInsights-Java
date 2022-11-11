plugins {
  id("ai.java-conventions")
  id("ai.javaagent-instrumentation")
}

muzzle {
  pass {
    coreJdk()
  }
}

val otelInstrumentationAlphaVersion: String by project

dependencies {
  compileOnly(project(":agent:instrumentation:azure-functions-worker-stub"))

  testImplementation(project(":agent:instrumentation:azure-functions-worker-stub"))

  // TODO remove when start using io.opentelemetry.instrumentation.javaagent-instrumentation plugin
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
}
