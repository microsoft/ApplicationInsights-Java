import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  java
  id("ai.spotless-conventions")
}

val aiSmokeTest = extensions.create<AiSmokeTestExtension>("aiSmokeTest")

sourceSets {
  create("smokeTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
  }
}

val smokeTestImplementation by configurations.getting {
  extendsFrom(configurations.implementation.get())
}

configurations["smokeTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

// FIXME (trask) copy-pasted from ai.java-conventions.gradle
java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }

  // See https://docs.gradle.org/current/userguide/upgrading_version_5.html, Automatic target JVM version
  disableAutoTargetJvm()
  withJavadocJar()
  withSourcesJar()
}

// FIXME (trask) copy-pasted from ai.java-conventions.gradle
tasks.withType<JavaCompile>().configureEach {
  with(options) {
    release.set(8)
    compilerArgs.add("-Werror")
  }
}

// FIXME (trask) copy-pasted from ai.java-conventions.gradle
val dependencyManagement by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = false
  isVisible = false
}
afterEvaluate {
  configurations.configureEach {
    if (isCanBeResolved && !isCanBeConsumed) {
      extendsFrom(dependencyManagement)
    }
  }
}

val agent by configurations.creating
val old3xAgent by configurations.creating

dependencies {
  // FIXME (trask) copy-pasted from ai.java-conventions.gradle
  dependencyManagement(platform(project(":dependencyManagement")))

  smokeTestImplementation(project(":smoke-tests:framework"))

  smokeTestImplementation("org.junit.jupiter:junit-jupiter-api")
  smokeTestImplementation("org.junit.jupiter:junit-jupiter-params")
  smokeTestImplementation("org.junit.jupiter:junit-jupiter-engine")
  smokeTestImplementation("org.junit.platform:junit-platform-launcher")

  smokeTestImplementation("org.assertj:assertj-core")

  agent(project(":agent:agent", configuration = "shadow"))

  old3xAgent("com.microsoft.azure:applicationinsights-agent:3.2.11")
}

configurations.all {
  // spring boot 2.x requires slf4j 1.x
  val slf4jVersion = "1.7.36"
  resolutionStrategy.force("org.slf4j:slf4j-api:${slf4jVersion}")
  resolutionStrategy.force("org.slf4j:log4j-over-slf4j:${slf4jVersion}")
  resolutionStrategy.force("org.slf4j:jcl-over-slf4j:${slf4jVersion}")
  resolutionStrategy.force("org.slf4j:jul-to-slf4j:${slf4jVersion}")
  resolutionStrategy.force("ch.qos.logback:logback-classic:1.2.12")
}

tasks {
  register<Test>("smokeTest") {
    useJUnitPlatform()

    // this is just to force building the agent first
    dependsOn(":agent:agent:shadowJar")

    dependsOn(assemble)

    testClassesDirs = sourceSets["smokeTest"].output.classesDirs
    classpath = sourceSets["smokeTest"].runtimeClasspath

    // TODO (trask) experiment with parallelization
    // maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1

    doFirst {

      val appFile = aiSmokeTest.testAppArtifactDir.file(aiSmokeTest.testAppArtifactFilename.get()).get()
      val javaagentFile = agent.singleFile
      val old3xJavaagentFile = old3xAgent.singleFile

      // need to delay for project to configure the extension
      systemProperty("ai.smoke-test.test-app-file", appFile)
      systemProperty("ai.smoke-test.javaagent-file", javaagentFile)
      systemProperty("ai.smoke-test.old-3x-javaagent-file", old3xJavaagentFile)

      val smokeTestMatrix = findProperty("smokeTestMatrix") ?: System.getenv("CI") != null
      systemProperty("ai.smoke-test.matrix", smokeTestMatrix)

      findProperty("smokeTestRemoteDebug")?.let { systemProperty("ai.smoke-test.remote-debug", it) }

      systemProperty("io.opentelemetry.context.enableStrictContext", true)
      systemProperty("io.opentelemetry.javaagent.shaded.io.opentelemetry.context.enableStrictContext", true)
    }

    testLogging {
      showStandardStreams = true
      exceptionFormat = TestExceptionFormat.FULL
    }

    // TODO (trask) this is still a problem
    //  e.g. changes in agent-tooling do not cause smoke tests to re-run
    outputs.upToDateWhen { false }
  }
}
