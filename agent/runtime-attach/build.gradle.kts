plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation("io.opentelemetry.contrib:opentelemetry-runtime-attach:1.14.0-alpha")
  implementation("com.microsoft.azure:applicationinsights-agent:" + "3.2.11")
  testImplementation("io.opentelemetry:opentelemetry-extension-annotations:1.14.0")
  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation("org.junit.jupiter:junit-jupiter-api:.8.2")
}
