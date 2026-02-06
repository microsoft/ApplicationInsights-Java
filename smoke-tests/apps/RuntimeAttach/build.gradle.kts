plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation(project(":agent:runtime-attach"))

  implementation("org.springframework:spring-webmvc:5.3.39")
  implementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.98")
  compileOnly("javax.servlet:javax.servlet-api:4.0.1")
}
