plugins {
  id("ai.java-conventions")
  id("ai.publish-conventions")
  id("ai.sdk-version-file")
}
base.archivesName.set("applicationinsights-runtime-attach")

val otelContribAlphaVersion: String by project
val otelVersion: String by project
val agent: Configuration by configurations.creating

dependencies {
  implementation("net.bytebuddy:byte-buddy-agent") // To replace with  implementation("io.opentelemetry.contrib:opentelemetry-runtime-attach:$otelContribAlphaVersion")
  // after next OTel java contrib release
  agent(project(":agent:agent", configuration = "shadow"))
}

tasks {
  jar {
    inputs.files(agent)
    from({
      agent.singleFile
    })
  }
}
