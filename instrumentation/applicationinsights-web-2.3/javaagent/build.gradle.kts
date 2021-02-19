plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("com.microsoft.azure")
    module.set("applicationinsights-web")
    versions.set("[2.3.0,)")
  }
}

dependencies {
  library("com.microsoft.azure:applicationinsights-web:2.3.0")

  testImplementation(project(":instrumentation:opentelemetry-annotations-1.0:javaagent"))

  testImplementation("io.opentelemetry:opentelemetry-extension-annotations")

  testImplementation("javax.servlet:javax.servlet-api:3.0.1")
}
