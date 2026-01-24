plugins {
  id("ai.smoke-test-war")
}

configurations.all {
  val slf4jVersion = "2.0.17"
  val logbackVersion = "1.5.20"
  resolutionStrategy.force("org.slf4j:slf4j-api:${slf4jVersion}")
  resolutionStrategy.force("org.slf4j:log4j-over-slf4j:${slf4jVersion}")
  resolutionStrategy.force("org.slf4j:jcl-over-slf4j:${slf4jVersion}")
  resolutionStrategy.force("org.slf4j:jul-to-slf4j:${slf4jVersion}")
  resolutionStrategy.force("ch.qos.logback:logback-classic:${logbackVersion}")
}

dependencies {
  implementation("ch.qos.logback:logback-classic")
}

