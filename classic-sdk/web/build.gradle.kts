plugins {
  id("ai.java-conventions")
  id("ai.publish-conventions")
}

base.archivesName.set("applicationinsights-web")

dependencies {
  api(project(":classic-sdk:core"))
  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
}

tasks {
  jar {
    manifest {
      attributes("Automatic-Module-Name" to "com.microsoft.applicationinsights.web")
    }
  }
}
