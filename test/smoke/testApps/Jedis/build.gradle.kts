plugins {
  id("ai.java-conventions")
  id("war")
}

war {
  // this is done to remove the version from the archive file name
  // to make span name verification simpler
  archiveFileName = project.name + ".war"
}

ext.testAppArtifactDir = war.destinationDirectory
ext.testAppArtifactFilename = project.name + ".war"

ext.dependencyContainers = ["redis"]

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web")
  implementation("redis.clients:jedis:2+")

  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
}
