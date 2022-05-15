plugins {
  id("ai.java-conventions")
  id("application")
}

dependencies {
  implementation("org.eclipse.jetty.aggregate:jetty-all:9.4.39.v20210325")
  implementation("com.google.code.gson:gson")
  implementation(project(":test:smoke:framework:utils"))
  implementation("com.google.guava:guava:30.1.1-jre")
}

application {
  mainClass.set("com.microsoft.applicationinsights.test.fakeingestion.MockedAppInsightsIngestionServer")
}
