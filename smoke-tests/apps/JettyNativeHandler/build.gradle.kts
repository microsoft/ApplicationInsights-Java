plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  // jetty 10 is compiled against Java 11
  implementation("org.eclipse.jetty:jetty-server:9.4.49.v20220914")
}
