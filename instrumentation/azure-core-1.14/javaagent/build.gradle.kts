plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("com.azure")
    module.set("azure-core")
    versions.set("[1.14.0,)")
    assertInverse.set(true)
  }
}

configurations {
  testRuntimeClasspath {
    exclude("com.azure", "azure-core-tracing-opentelemetry")
  }
}

dependencies {
  // to check for (potentially incompatible) differences in new versions of the injected artifact, run:
  // git diff azure-core-tracing-opentelemetry_1.0.0-beta.8 azure-core-tracing-opentelemetry_1.0.0-beta.13
  //          -- sdk/core/azure-core-tracing-opentelemetry
  implementation("com.azure:azure-core-tracing-opentelemetry:1.0.0-beta.13") {
    exclude("com.azure", "azure-core")
  }

  library("com.azure:azure-core:1.14.0")
}
