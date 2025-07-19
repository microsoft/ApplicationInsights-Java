plugins {
  id("ai.java-conventions")
  id("ai.shadow-conventions")

  id("io.opentelemetry.instrumentation.muzzle-generation")
  id("io.opentelemetry.instrumentation.muzzle-check")
}

val otelInstrumentationAlphaVersion: String by project

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")
  compileOnly(project(":agent:agent-bootstrap"))
}

// Fix configuration cache compatibility for ByteBuddy tasks
tasks.matching { it.name == "byteBuddyJava" }.configureEach {
  notCompatibleWithConfigurationCache("ByteBuddy tasks access Task.project at execution time")
}
