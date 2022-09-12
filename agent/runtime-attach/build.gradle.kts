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
  implementation("io.opentelemetry.contrib:opentelemetry-runtime-attach-core:$otelContribAlphaVersion")
  agent(project(":agent:agent", configuration = "shadow"))
}

tasks {
  jar {
    inputs.files(agent)
    from({
      agent.singleFile
    })
    manifest {
      attributes("Automatic-Module-Name" to "com.microsoft.applicationinsights.attach")
    }
  }
}
