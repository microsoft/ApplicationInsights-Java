plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework:spring-webmvc:5.3.39")
  implementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.98")
  compileOnly("javax.servlet:javax.servlet-api:4.0.1")

  implementation("io.opentelemetry:opentelemetry-api:1.12.0")

  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("ch.qos.logback:logback-classic:1.2.12")
}
