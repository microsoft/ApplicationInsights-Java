plugins {
  id("ai.java-conventions")
  id("ai.publish-conventions")
  // FIXME (trask) add artifact name to version file to avoid conflicts
  id("ai.sdk-version-file")
}

base.archivesName.set("applicationinsights-core")

dependencies {
  compileOnly("com.github.spotbugs:spotbugs-annotations")
}
