import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.time.Duration

plugins {
  `java-library`
  checkstyle
  idea

  id("org.gradle.test-retry")

  id("ai.errorprone-conventions")
  id("ai.spotless-conventions")
}

// Version to use to compile code and run tests.
val DEFAULT_JAVA_VERSION = JavaVersion.VERSION_11

java {
  // FIXME (trask)
//  toolchain {
//    languageVersion.set(DEFAULT_JAVA_VERSION.majorVersion.toInt())
//  }

  // See https://docs.gradle.org/current/userguide/upgrading_version_5.html, Automatic target JVM version
  disableAutoTargetJvm()
  withJavadocJar()
  withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
  with(options) {
    release.set(8)
    compilerArgs.add("-Werror")
  }
}

evaluationDependsOn(":dependencyManagement")
val dependencyManagementConf = configurations.create("dependencyManagement") {
  isCanBeConsumed = false
  isCanBeResolved = false
  isVisible = false
}
afterEvaluate {
  configurations.configureEach {
    if (isCanBeResolved && !isCanBeConsumed) {
      extendsFrom(dependencyManagementConf)
    }
  }
}

dependencies {
  add(dependencyManagementConf.name, platform(project(":dependencyManagement")))

  compileOnly("org.checkerframework:checker-qual")

  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine")

  testImplementation("ch.qos.logback:logback-classic")
  testImplementation("org.slf4j:log4j-over-slf4j")
  testImplementation("org.slf4j:jcl-over-slf4j")
  testImplementation("org.slf4j:jul-to-slf4j")
}

tasks {
  named<Jar>("jar") {
    // By default Gradle Jar task can put multiple files with the same name
    // into a Jar. This may lead to confusion. For example if auto-service
    // annotation processing creates files with same name in `scala` and
    // `java` directory this would result in Jar having two files with the
    // same name in it. Which in turn would result in only one of those
    // files being actually considered when that Jar is used leading to very
    // confusing failures. Instead we should 'fail early' and avoid building such Jars.
    duplicatesStrategy = DuplicatesStrategy.FAIL
  }

  withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
  }
}

normalization {
  runtimeClasspath {
    metaInf {
      ignoreAttribute("Implementation-Version")
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()

  // There's no real harm in setting this for all tests even if any happen to not be using context
  // propagation.
  jvmArgs("-Dio.opentelemetry.context.enableStrictContext=true")
  // TODO (trask): Have agent map unshaded to shaded.
  jvmArgs("-Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.enableStrictContext=true")

  // All tests must complete within 15 minutes.
  // This value is quite big because with lower values (3 mins) we were experiencing large number of false positives
  timeout.set(Duration.ofMinutes(15))

  retry {
    // You can see tests that were retried by this mechanism in the collected test reports and build scans.
    maxRetries.set(if (System.getenv("CI") != null) 5 else 0)
  }

  reports {
    junitXml.isOutputPerTestCase = true
  }

  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
  }
}

checkstyle {
  configFile = rootProject.file("gradle/enforcement/checkstyle.xml")
  // this version should match the version of google_checks.xml used as basis for above configuration
  toolVersion = "8.37"
  maxWarnings = 0
}
