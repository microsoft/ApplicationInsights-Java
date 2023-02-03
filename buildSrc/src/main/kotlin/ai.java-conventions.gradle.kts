import com.gradle.enterprise.gradleplugin.testretry.retry
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.time.Duration

plugins {
  `java-library`
  checkstyle
  idea

  id("ai.errorprone-conventions")
  id("ai.spotless-conventions")
  id("ai.spotbugs-conventions")
  id("org.owasp.dependencycheck")
}

repositories {
  mavenCentral()
  mavenLocal()
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }

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

dependencies {
  dependencyManagement(platform(project(":dependencyManagement")))

  compileOnly("com.google.code.findbugs:jsr305")
  compileOnly("com.google.errorprone:error_prone_annotations")
  compileOnly("com.github.spotbugs:spotbugs-annotations")

  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

  testImplementation("ch.qos.logback:logback-classic")
  testImplementation("org.slf4j:log4j-over-slf4j")
  testImplementation("org.slf4j:jcl-over-slf4j")
  testImplementation("org.slf4j:jul-to-slf4j")
}

configurations.configureEach {
  resolutionStrategy {
    failOnVersionConflict()
    preferProjectModules()
  }
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

  // All tests must complete within 15 minutes.
  // This value is quite big because with lower values (3 mins) we were experiencing large number of false positives
  timeout.set(Duration.ofMinutes(15))

  retry {
    // You can see tests that were retried by this mechanism in the collected test reports and build scans.
    if (System.getenv().containsKey("CI")) {
      maxRetries.set(5)
    }
  }

  reports {
    junitXml.isOutputPerTestCase = true
  }

  testLogging {
    showStandardStreams = true
    exceptionFormat = TestExceptionFormat.FULL
  }
}

afterEvaluate {
  val testJavaVersion = gradle.startParameter.projectProperties["testJavaVersion"]?.let(JavaVersion::toVersion)
  val useJ9 = gradle.startParameter.projectProperties["testJavaVM"]?.run { this == "openj9" }
    ?: false
  tasks.withType<Test>().configureEach {
    if (testJavaVersion != null) {
      javaLauncher.set(
        javaToolchains.launcherFor {
          languageVersion.set(JavaLanguageVersion.of(testJavaVersion.majorVersion))
          implementation.set(if (useJ9) JvmImplementation.J9 else JvmImplementation.VENDOR_SPECIFIC)
        }
      )
    }
  }
}

checkstyle {
  configFile = rootProject.file("buildscripts/checkstyle.xml")
  // this version should match the version of google_checks.xml used as basis for above configuration
  toolVersion = "8.37"
  maxWarnings = 0
}

dependencyCheck {
  skipConfigurations = listOf("errorprone", "spotbugs", "checkstyle", "annotationProcessor")
  failBuildOnCVSS = 0f // fail on any reported CVE
  suppressionFile = rootProject.file("buildscripts/dependency-check-suppressions.xml").absolutePath;
}

if (!path.startsWith(":smoke-tests")) {
  configurations.configureEach {
    if (name.toLowerCase().endsWith("runtimeclasspath")) {
      resolutionStrategy.activateDependencyLocking()
    }
  }
}

// see https://docs.gradle.org/current/userguide/dependency_locking.html#lock_all_configurations_in_one_build_execution
tasks.register("resolveAndLockAll") {
  doFirst {
    require(gradle.startParameter.isWriteDependencyLocks)
  }
  doLast {
    if (configurations.findByName("runtimeClasspath") != null) {
      configurations.named("runtimeClasspath").get().resolve()
    }
  }
}
