plugins {
  `java-library`
}

val sdkVersionDir = layout.buildDirectory.dir("generated/resources/sdk-version")

tasks {
  register("generateVersionResource") {
    inputs.property("project.version", project.version.toString())
    outputs.dir(sdkVersionDir)

    doLast {
      sdkVersionDir.get().file("ai.sdk-version.properties").asFile.writeText("version=${project.version}")
    }
  }
}

sourceSets {
  main {
    output.dir(sdkVersionDir, "builtBy" to "generateVersionResource")
  }
}
