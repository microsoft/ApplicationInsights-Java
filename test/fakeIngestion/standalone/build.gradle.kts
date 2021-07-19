plugins {
  id("ai.java-conventions")
  id("application")
}

dependencies {
  implementation("org.eclipse.jetty.aggregate:jetty-all:9.4.39.v20210325")
  implementation("com.google.code.gson:gson")
  implementation(project(":test:fakeIngestion:servlet"))
  implementation(project(":test:smoke:framework:utils"))
}

application {
  mainClass.set("com.microsoft.applicationinsights.test.fakeingestion.MockedAppInsightsIngestionServer")
}
