import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("ai.java-conventions")
  id("ai.shadow-conventions")

  id("io.opentelemetry.instrumentation.muzzle-generation")
  id("io.opentelemetry.instrumentation.muzzle-check")
}

val otelInstrumentationAlphaVersion: String by project

val testInstrumentation by configurations.creating

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")
  compileOnly(project(":agent:agent-bootstrap"))

  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common:$otelInstrumentationAlphaVersion")
  // the bootstrap module is provided by the javaagent in the instrumentation test runtime, no need to include it
  // (especially when it's not being shaded)
  testCompileOnly(project(":agent:agent-bootstrap"))
}

tasks.named<ShadowJar>("shadowJar").configure {
  configurations = listOf(project.configurations.runtimeClasspath.get(), testInstrumentation)

  archiveFileName.set("agent-testing.jar")
}

evaluationDependsOn(":agent:agent-for-testing")

tasks.withType<Test>().configureEach {
  val shadowJar = tasks.shadowJar.get()
  val agentShadowJar = project(":agent:agent-for-testing").tasks.shadowJar.get()

  inputs.file(shadowJar.archiveFile)

  dependsOn(shadowJar)
  dependsOn(agentShadowJar)

  jvmArgs("-Dotel.javaagent.debug=true")
  jvmArgs("-javaagent:${agentShadowJar.archiveFile.get().asFile.absolutePath}")
  jvmArgs("-Dotel.javaagent.experimental.initializer.jar=${shadowJar.archiveFile.get().asFile.absolutePath}")
  jvmArgs("-Dotel.javaagent.testing.additional-library-ignores.enabled=false")
  jvmArgs("-Dotel.javaagent.testing.fail-on-context-leak=true")
  // prevent sporadic gradle deadlocks, see SafeLogger for more details
  jvmArgs("-Dotel.javaagent.testing.transform-safe-logging.enabled=true")
  // Reduce noise in assertion messages since we don't need to verify this in most tests. We check
  // in smoke tests instead.
  jvmArgs("-Dotel.javaagent.add-thread-details=false")
  // needed for proper GlobalMeterProvider registration
  jvmArgs("-Dotel.metrics.exporter=otlp")

  // The sources are packaged into the testing jar so we need to make sure to exclude from the test
  // classpath, which automatically inherits them, to ensure our shaded versions are used.
  classpath = classpath.filter {
    if (file("$buildDir/resources/main") == it || file("$buildDir/classes/java/main") == it) {
      return@filter false
    }
    return@filter true
  }
}
