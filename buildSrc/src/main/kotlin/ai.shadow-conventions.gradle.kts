import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("com.github.johnrengelman.shadow")
}

tasks.withType<ShadowJar>().configureEach {
  mergeServiceFiles {
    include("inst/META-INF/services/*")
  }

  exclude("**/module-info.class")

  // Prevents conflict with other SLF4J instances. Important for premain.
  relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")

  // rewrite dependencies calling Logger.getLogger
  relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

  // prevents conflict with library instrumentation, since these classes live in the bootstrap class loader
  relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
    // Exclude resource providers since they live in the agent class loader
    exclude("io.opentelemetry.instrumentation.resources.*")
    exclude("io.opentelemetry.instrumentation.spring.resources.*")
  }

  // relocate(OpenTelemetry API) since these classes live in the bootstrap class loader
  relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
  relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
  relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")

  // relocate(the OpenTelemetry extensions that are used by instrumentation modules)
  // these extensions live in the AgentClassLoader, and are injected into the user's class loader
  // by the instrumentation modules that use them
  relocate("io.opentelemetry.contrib.awsxray", "io.opentelemetry.javaagent.shaded.io.opentelemetry.contrib.awsxray")
  relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")
  relocate("io.opentelemetry.extension.incubator", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.incubator")

  val shadowPrefix = "com.microsoft.applicationinsights.agent.shadow"

  relocate("ch.qos.logback", "$shadowPrefix.ch.qos.logback")
  relocate("javax.annotation", "$shadowPrefix.javax.annotation")

  // to prevent accidentally picking up from user's class path
  relocate("logback.configurationFile", "applicationinsights.logback.configurationFile")
  relocate("logback.xml", "applicationinsights.logback.xml")
  relocate("logback.groovy", "applicationinsights.logback.groovy")
  relocate("logback-test.xml", "applicationinsights.logback-test.xml")
}
