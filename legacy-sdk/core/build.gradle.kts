plugins {
  id("ai.java-conventions")
  id("ai.publish-conventions")
}

base.archivesName.set("applicationinsights-core")

dependencies {
  compileOnly("com.github.spotbugs:spotbugs-annotations")
}
