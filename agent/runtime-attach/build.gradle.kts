plugins {
  id("ai.java-conventions")
}

val otelInstrumentationAlphaVersion: String by project
val otelVersion: String by project

dependencies {
  implementation("io.opentelemetry.contrib:opentelemetry-runtime-attach:$otelInstrumentationAlphaVersion")
  implementation(project(":agent:agent", configuration = "shadow"))
  testImplementation("io.opentelemetry:opentelemetry-extension-annotations:$otelVersion")
  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation("org.junit.jupiter:junit-jupiter-api:.8.2")
}
