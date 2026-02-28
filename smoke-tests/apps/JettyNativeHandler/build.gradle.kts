plugins {
  id("ai.smoke-test-jar")
}

// Override default main class
ext.set("mainClassName", "com.microsoft.applicationinsights.smoketestapp.JettyNativeHandlerApp")

dependencies {
  implementation("org.springframework.boot:spring-boot-starter:2.5.12")

  // jetty 10 is compiled against Java 11
  implementation("org.eclipse.jetty:jetty-server:9.4.49.v20220914")
}
