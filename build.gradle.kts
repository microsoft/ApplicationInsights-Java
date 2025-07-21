plugins {
  id("idea")

  id("ai.spotless-conventions")
}

val isRelease = (System.getProperty("isRelease") ?: "false").toBoolean()
if (!isRelease) {
  version = "$version-SNAPSHOT"
  logger.info("This is NOT a release version; version updated to $version")
}

subprojects {
  version = rootProject.version
}

extra["isRelease"] = (System.getProperty("isRelease") ?: "false").toBoolean()
