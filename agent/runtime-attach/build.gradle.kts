plugins {
  id("ai.java-conventions")
  id("ai.publish-conventions")
}

base.archivesName.set("applicationinsights-runtime-attach")

val otelContribAlphaVersion: String by project
val otelVersion: String by project

dependencies {
  implementation("io.opentelemetry.contrib:opentelemetry-runtime-attach:$otelContribAlphaVersion")
  implementation(project(":agent:agent", configuration = "shadow"))
  testImplementation("io.opentelemetry:opentelemetry-extension-annotations:$otelVersion")
  testImplementation("org.assertj:assertj-core")
}
