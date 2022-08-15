plugins {
  id("ai.java-conventions")
  id("ai.publish-conventions")
}

base.archivesName.set("applicationinsights-web")

dependencies {
  api(project(":classic-sdk:core"))
  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
}
