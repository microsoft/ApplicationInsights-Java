plugins {
  id("otel.javaagent-instrumentation")
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

dependencies {
  library("io.micrometer:micrometer-core:1.4.0")
  library("org.springframework.boot:spring-boot-actuator-autoconfigure:2.2.0.RELEASE")
  compileOnly(project(":javaagent-bootstrap"))

  testImplementation("com.microsoft.azure:azure-spring-boot-metrics-starter:2.2.3")
}
