import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.process.CommandLineArgumentProvider

plugins {
  id("ai.javaagent-instrumentation-base")
}

val otelInstrumentationAlphaVersion: String by project

val testInstrumentation by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = true
}

val agentForTesting by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = true
}

class JavaagentTestArgumentsProvider(
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val agentShadowJar: File,

  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val shadowJar: File,
) : CommandLineArgumentProvider {
  override fun asArguments(): Iterable<String> = listOf(
    "-Dotel.javaagent.debug=true",
    "-javaagent:${agentShadowJar.absolutePath}",
    // make the path to the javaagent available to tests
    "-Dotel.javaagent.testing.javaagent-jar-path=${agentShadowJar.absolutePath}",
    "-Dotel.javaagent.experimental.initializer.jar=${shadowJar.absolutePath}",
    "-Dotel.javaagent.testing.additional-library-ignores.enabled=false",
    "-Dotel.javaagent.testing.fail-on-context-leak=true",
    // prevent sporadic gradle deadlocks, see SafeLogger for more details
    "-Dotel.javaagent.testing.transform-safe-logging.enabled=true",
    // Reduce noise in assertion messages since we don't need to verify this in most tests. We check
    // in smoke tests instead.
    "-Dotel.javaagent.add-thread-details=false",
    // needed for proper GlobalMeterProvider registration
    "-Dotel.metrics.exporter=otlp"
  )
}

dependencies {
  // Use your local agent-for-testing instead of the upstream one
  agentForTesting(project(":agent:agent-for-testing"))
  
  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common:$otelInstrumentationAlphaVersion")
  // the bootstrap module is provided by the javaagent in the instrumentation test runtime, no need to include it
  // (especially when it's not being shaded)
  testCompileOnly(project(":agent:agent-bootstrap"))
}

tasks.named<ShadowJar>("shadowJar").configure {
  configurations = listOf(project.configurations.runtimeClasspath.get(), testInstrumentation)

  archiveFileName.set("agent-testing.jar")
}

// need to run this after evaluate because testSets plugin adds new test tasks
afterEvaluate {
  tasks.withType<Test>().configureEach {
    val shadowJar = tasks.shadowJar.get()
    val agentShadowJar = project(":agent:agent-for-testing").tasks.shadowJar.get().archiveFile.get().asFile

    dependsOn(shadowJar)
    dependsOn(project(":agent:agent-for-testing").tasks.shadowJar)

    jvmArgumentProviders.add(
      JavaagentTestArgumentsProvider(
        agentShadowJar,
        shadowJar.archiveFile.get().asFile
      )
    )
  }
}
