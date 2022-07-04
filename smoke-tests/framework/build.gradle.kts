plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation("com.google.guava:guava")
  implementation("org.junit.jupiter:junit-jupiter")
  implementation("org.apache.commons:commons-lang3:3.12.0")

  implementation("com.google.code.gson:gson")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
  implementation("org.hamcrest:hamcrest-library:2.2")

  implementation("org.eclipse.jetty.aggregate:jetty-all:9.4.39.v20210325")

  implementation("org.testcontainers:testcontainers:1.17.3")

  implementation("org.awaitility:awaitility:4.2.0")

  implementation("ch.qos.logback:logback-classic")

  testImplementation("org.assertj:assertj-core")
}
