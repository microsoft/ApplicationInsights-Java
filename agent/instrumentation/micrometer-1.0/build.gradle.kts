plugins {
  id("ai.java-conventions")
  id("ai.javaagent-instrumentation")
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

val otelInstrumentationAlphaVersion: String by project

dependencies {
  compileOnly("io.micrometer:micrometer-core:1.0.0")
  compileOnly("org.springframework.boot:spring-boot-actuator-autoconfigure:2.2.0.RELEASE")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:$otelInstrumentationAlphaVersion")

  testImplementation("com.microsoft.azure:azure-spring-boot-metrics-starter:2.2.3") {
    exclude("io.micrometer", "micrometer-core")
  }

  testImplementation("io.micrometer:micrometer-core:1.1.0")

  // TODO remove when start using io.opentelemetry.instrumentation.javaagent-instrumentation plugin
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
}

tasks {
  withType<Test>().configureEach {
    jvmArgs("-Dapplicationinsights.internal.micrometer.step.millis=100")
  }
}
