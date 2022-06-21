plugins {
  id("ai.java-conventions")
  id("org.springframework.boot" version "2.1.7.RELEASE")
  id("war")
}

dependencies {
  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
  // TODO (trask) ETW: is this project really needed? if so, need to restore devtest configuration
  // compileOnly(project(path:":agent:agent", configuration:"devtest"))
  compileOnly(project(path: ":agent:agent"))

  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation("org.apache.commons:commons-lang3:3.12.0")
}
