plugins {
  id("groovy")

  id("ai.java-conventions")
  id("ai.javaagent-instrumentation")

  id("io.opentelemetry.instrumentation.muzzle-generation") version "1.13.1-alpha"
  id("io.opentelemetry.instrumentation.muzzle-check") version "1.13.1-alpha"
}

muzzle {
  pass {
    group.set("io.micrometer")
    module.set("micrometer-core")
    versions.set("[1.0.0,)")
    extraDependency("org.springframework.boot:spring-boot-actuator-autoconfigure:2.0.0.RELEASE")
    assertInverse.set(true)
  }
  pass {
    group.set("org.springframework.boot")
    module.set("spring-boot-actuator-autoconfigure")
    versions.set("[2.0.0.RELEASE,)")
    extraDependency("io.micrometer:micrometer-core:1.0.0")
    assertInverse.set(true)
  }
}

val otelInstrumentationVersionAlpha: String by project

dependencies {
  compileOnly("io.micrometer:micrometer-core:1.0.0")
  compileOnly("org.springframework.boot:spring-boot-actuator-autoconfigure:2.2.0.RELEASE")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:$otelInstrumentationVersionAlpha")

  testImplementation("com.microsoft.azure:azure-spring-boot-metrics-starter:2.2.3") {
    exclude("io.micrometer", "micrometer-core")
  }

  testImplementation("io.micrometer:micrometer-core:1.1.0")

  // TODO remove when start using io.opentelemetry.instrumentation.javaagent-instrumentation plugin
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersionAlpha")
  add("codegen", "ch.qos.logback:logback-classic:1.2.3")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-annotation-support:$otelInstrumentationVersionAlpha")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationVersionAlpha")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersionAlpha")
}

tasks {
  withType<Test>().configureEach {
    jvmArgs("-Dotel.micrometer.step.millis=100")
  }
}
