plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web")

  providedRuntime("mysql:mysql-connector-java:5.1.44")
}
