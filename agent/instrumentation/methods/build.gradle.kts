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
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  // TODO remove when start using io.opentelemetry.instrumentation.javaagent-instrumentation plugin
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
}

tasks.withType<Test>().configureEach {
  jvmArgs(
    "-Dapplicationinsights.internal.methods.include=io.opentelemetry.javaagent.instrumentation.methods.ai.MethodTest\$ConfigTracedCallable[call];io.opentelemetry.javaagent.instrumentation.methods.ai.MethodTest\$ConfigTracedCompletableFuture[getResult]"
  )
}
