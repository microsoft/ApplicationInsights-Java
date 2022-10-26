plugins {
  id("idea")

  id("com.github.ben-manes.versions")
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

allprojects {

  if (!path.startsWith(":smoke-tests")) {
    configurations.configureEach {
      if (name.toLowerCase().endsWith("runtimeclasspath")) {
        resolutionStrategy.activateDependencyLocking()
      }
    }
  }

  tasks.register("generateLockfiles") {
    doFirst {
      // you must run with --write-locks parameter
      require(gradle.startParameter.isWriteDependencyLocks)
    }
    doLast {
      if (configurations.findByName("runtimeClasspath") != null) {
        configurations.named("runtimeClasspath").get().resolve()
      }
    }
  }
}
