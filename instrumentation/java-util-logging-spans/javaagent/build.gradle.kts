plugins {
  id("otel.javaagent-instrumentation")
}

dependencies {
  compileOnly(project(":java-util-logging-logger-shaded-for-instrumenting"))

  testLibrary("org.jboss.logmanager:jboss-logmanager:1.0.0.GA")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.experimental.log.capture.threshold=warn")
}
