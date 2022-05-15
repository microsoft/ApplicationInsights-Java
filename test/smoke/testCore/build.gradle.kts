plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation("com.google.guava:guava")
  implementation("junit:junit:4.13.2")
  implementation("org.apache.commons:commons-lang3:3.7")

  implementation("com.google.code.gson:gson")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
  implementation("org.hamcrest:hamcrest-library:1.3")

  implementation("org.eclipse.jetty.aggregate:jetty-all:9.4.39.v20210325")

  testImplementation("org.assertj:assertj-core")
}
