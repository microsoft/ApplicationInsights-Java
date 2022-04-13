plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("ch.qos.logback")
    module.set("logback-classic")
    versions.set("[0.9.16,)")
  }
}

dependencies {
  compileOnly("ch.qos.logback:logback-classic:0.9.16")

  testImplementation("ch.qos.logback:logback-classic:0.9.16")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.experimental.log.capture.threshold=warn")
}
