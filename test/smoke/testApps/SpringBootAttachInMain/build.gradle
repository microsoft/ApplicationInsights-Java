plugins {
  id("ai.java-conventions")
  id "org.springframework.boot" version "2.2.0.RELEASE"
}

ext.testAppArtifactDir = jar.destinationDirectory
ext.testAppArtifactFilename = jar.archiveFileName.get()

dependencies {
  implementation(project(":agent:agent"))

  implementation("org.springframework.boot:spring-boot-starter-web:2.2.0.RELEASE")
  implementation("net.bytebuddy:byte-buddy-agent:1.11.0")
}
