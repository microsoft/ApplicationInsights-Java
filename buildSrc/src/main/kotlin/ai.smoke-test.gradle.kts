import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  java
  id("ai.spotless-conventions")
}

val aiSmokeTest = extensions.create<AiSmokeTestExtension>("aiSmokeTest")

// FIXME (trask) copy-pasted from ai.java-conventions.gradle
java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
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
    // We need to support compiling to Java 8.
    // Suppress obsolete source/target warning added in JDK 21 while retaining -Werror for everything else.
    // This only disables the 'options' lint category (e.g., the obsolete source/target messages).
    compilerArgs.add("-Xlint:-options")
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

  agent(project(":agent:agent", configuration = "shadow"))

  old3xAgent("com.microsoft.azure:applicationinsights-agent:3.2.11")
}

// Configure test suites
testing {
  suites {
    register<JvmTestSuite>("smokeTest") {
      dependencies {
        implementation(project(":smoke-tests:framework"))

        implementation("org.junit.jupiter:junit-jupiter-api")
        implementation("org.junit.jupiter:junit-jupiter-params")
        runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
        runtimeOnly("org.junit.platform:junit-platform-launcher")

        implementation("org.assertj:assertj-core")
      }
    }
  }
}

// Make smokeTest configuration extend from main implementation configuration
// so that project dependencies (e.g., log4j in ClassicSdkLog4j1Interop2x) are available
configurations.named("smokeTestImplementation") {
  extendsFrom(configurations.implementation.get())
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
  // FIXME (trask) copy-pasted from ai.java-conventions.gradle
  named<Javadoc>("javadoc") {
    with(options as StandardJavadocDocletOptions) {
      source = "8"
      encoding = "UTF-8"
      docEncoding = "UTF-8"
      charSet = "UTF-8"
      breakIterator(true)

      addStringOption("Xdoclint:none", "-quiet")
      // non-standard option to fail on warnings, see https://bugs.openjdk.java.net/browse/JDK-8200363
      addStringOption("Xwerror", "-quiet")
    }
  }

  named<Test>("smokeTest") {
    useJUnitPlatform()

    // this is just to force building the agent first
    dependsOn(":agent:agent:shadowJar")

    dependsOn(assemble)

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
