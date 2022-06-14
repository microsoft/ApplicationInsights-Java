plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation("io.opentelemetry.contrib:opentelemetry-runtime-attach:1.14.0-alpha")
  implementation(project(":agent:agent", configuration = "shadow"))
  testImplementation("io.opentelemetry:opentelemetry-extension-annotations:1.14.0")
  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation("org.junit.jupiter:junit-jupiter-api:.8.2")
}
