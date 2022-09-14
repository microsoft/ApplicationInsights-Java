plugins {
  id("ai.java-conventions")
  id("ai.publish-conventions")
}

base.archivesName.set("applicationinsights-core")

dependencies {
  compileOnly("com.github.spotbugs:spotbugs-annotations")
}

tasks {
  jar {
    manifest {
      attributes("Automatic-Module-Name" to "com.microsoft.applicationinsights")
    }
  }
}
