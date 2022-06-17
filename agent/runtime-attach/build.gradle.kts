plugins {
  id("ai.java-conventions")
}

val otelContribAlphaVersion: String by project
val otelVersion: String by project

dependencies {
  implementation("io.opentelemetry.contrib:opentelemetry-runtime-attach:$otelContribAlphaVersion")
  implementation(project(":agent:agent", configuration = "shadow"))
  implementation("net.bytebuddy:byte-buddy-agent") // To remove after next Java contrib release
  testImplementation("io.opentelemetry:opentelemetry-extension-annotations:$otelVersion")
  testImplementation("org.assertj:assertj-core")
}
