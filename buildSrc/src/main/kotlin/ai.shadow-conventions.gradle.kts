import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

plugins {
  id("com.gradleup.shadow")
}

tasks.withType<ShadowJar>().configureEach {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  mergeServiceFiles()
  mergeServiceFiles("META-INF/services/**")
  mergeServiceFiles {
    include("inst/META-INF/services/**")
  }
  filesMatching("META-INF/services/**") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
  filesMatching("inst/META-INF/services/**") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
  exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
  exclude(
    "META-INF/LICENSE",
    "META-INF/NOTICE",
    "META-INF/LICENSE.txt",
    "META-INF/NOTICE.txt",
    "META-INF/INDEX.LIST",
    "META-INF/io.netty.versions.properties",
    "META-INF/AL2.0",
    "META-INF/LGPL2.1"
  )
  exclude("META-INF/maven/**")

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
  relocate("io.opentelemetry.common", "io.opentelemetry.javaagent.shaded.io.opentelemetry.common")

  // relocate(the OpenTelemetry extensions that are used by instrumentation modules)
  // these extensions live in the AgentClassLoader, and are injected into the user's class loader
  // by the instrumentation modules that use them
  relocate("io.opentelemetry.contrib.awsxray", "io.opentelemetry.javaagent.shaded.io.opentelemetry.contrib.awsxray")
  relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")

  val shadowPrefix = "com.microsoft.applicationinsights.agent.shadow"

  relocate("ch.qos.logback", "$shadowPrefix.ch.qos.logback")
  relocate("javax.annotation", "$shadowPrefix.javax.annotation")

  // to prevent accidentally picking up from user's class path
  relocate("logback.configurationFile", "applicationinsights.logback.configurationFile")
  relocate("logback.xml", "applicationinsights.logback.xml")
  relocate("logback.groovy", "applicationinsights.logback.groovy")
  relocate("logback-test.xml", "applicationinsights.logback-test.xml")
}
