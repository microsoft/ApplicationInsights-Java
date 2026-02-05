plugins {
  `java-library`
}

val sdkVersionDir = "${buildDir}/generated/resources/sdk-version"

tasks {
  register("generateVersionResource") {
    inputs.property("project.version", project.version.toString())
    outputs.dir(sdkVersionDir)

    doLast {
      File(sdkVersionDir, "ai.sdk-version.properties").writeText("version=${project.version}")
    }
  }
}

sourceSets {
  main {
    output.dir(sdkVersionDir, "builtBy" to "generateVersionResource")
  }
}
