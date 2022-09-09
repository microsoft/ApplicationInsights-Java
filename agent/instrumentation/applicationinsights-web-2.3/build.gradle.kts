plugins {
  id("ai.java-conventions")
  id("ai.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("com.microsoft.azure")
    module.set("applicationinsights-web")
    versions.set("[2.3.0,)")
  }
}

val otelInstrumentationAlphaVersion: String by project
val otelVersion: String by project

dependencies {
  compileOnly("com.microsoft.azure:applicationinsights-web:2.3.0")

  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:$otelVersion")

  testImplementation("com.microsoft.azure:applicationinsights-web:2.3.0")
  testImplementation("javax.servlet:javax.servlet-api:3.0.1")

  add("codegen", "io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.0:$otelInstrumentationAlphaVersion")

  // TODO remove when start using io.opentelemetry.instrumentation.javaagent-instrumentation plugin
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
}
