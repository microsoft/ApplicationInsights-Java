plugins {
  `java-library`
}

val sdkVersionDir = "${buildDir}/generated/resources/sdk-version"

sourceSets {
  main {
    resources {
      srcDir(sdkVersionDir)
    }
  }
}

tasks {
  val generateVersionProperties by registering(WriteProperties::class) {
    outputFile = File(sdkVersionDir, "ai.sdk-version.properties")
    property("version", project.version)
  }
  processResources {
    dependsOn(generateVersionProperties)
  }
}
