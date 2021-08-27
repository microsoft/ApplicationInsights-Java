plugins {
  id("ai.java-conventions")
  id("com.github.johnrengelman.shadow")
}

val otelInstrumentationAlphaVersion = "1.5.0+ai.patches-alpha"

configurations {
  // exclude bootstrap dependencies from shadowJar
  implementation {
    exclude("org.slf4j")
    exclude("io.opentelemetry", "opentelemetry-api")
    exclude("io.opentelemetry", "opentelemetry-api-metrics")
    exclude("io.opentelemetry", "opentelemetry-context")
    exclude("io.opentelemetry", "opentelemetry-context-prop")
    exclude("io.opentelemetry", "opentelemetry-semconv")

    exclude("io.opentelemetry.instrumentation", "opentelemetry-instrumentation-api")

    // TODO (trask) Azure SDK: why is this included in azure-core?
    exclude("io.netty", "netty-tcnative-boringssl-static")

    // this is pulled in via azure-core-http-netty ==> reactor-netty
    exclude("io.projectreactor.netty", "reactor-netty-http-brave")
  }
}

dependencies {
  implementation(project(":agent:agent-tooling")) {
    // exclude bootstrap dependencies from shadowJar

    exclude("io.opentelemetry.javaagent", "opentelemetry-javaagent-bootstrap")
    exclude("io.opentelemetry.javaagent", "opentelemetry-javaagent-instrumentation-api")

    exclude("io.opentelemetry", "opentelemetry-context")
    exclude("io.opentelemetry", "opentelemetry-context-prop")

    exclude("ch.qos.logback", "logback-classic")
    exclude("ch.qos.logback", "logback-core")
  }

  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpasyncclient-4.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpclient-2.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpclient-4.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-apache-httpclient-5.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-applicationinsights-web-2.3:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-async-http-client-1.9:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-async-http-client-2.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-azure-functions:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-azure-core-1.14:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-azure-core-1.19:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-cassandra-3.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-cassandra-4.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-executors:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-google-http-client-1.19:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-grizzly-2.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-grpc-1.6:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-guava-10.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-http-url-connection:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-internal-class-loader:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-internal-eclipse-osgi-3.6:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-internal-proxy:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-internal-url-class-loader:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-java-http-client:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-java-util-logging-spans:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-1.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-2.0-cxf-3.2:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-2.0-jersey-2.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-2.0-resteasy-3.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-2.0-resteasy-3.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-client-1.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-client-2.0-cxf-3.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-client-2.0-jersey-2.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxrs-client-2.0-resteasy-3.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxws-2.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxws-2.0-axis2-1.6:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxws-2.0-cxf-3.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxws-2.0-metro-2.2:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jdbc:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jedis-1.4:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jedis-3.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jetty-8.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jetty-11.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jetty-httpclient-9.2:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jms-1.1:${otelInstrumentationAlphaVersion}")
  // TODO (trask) add this once able to disable INTERNAL spans
  // implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jaxws-jws-1.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-kafka-clients-0.11:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-kafka-streams-0.11:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-kotlinx-coroutines:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-lettuce-4.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-lettuce-5.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-lettuce-5.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-log4j-spans-1.2:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-log4j-spans-2.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-logback-spans-1.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-micrometer-1.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-mongo-3.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-mongo-3.7:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-mongo-4.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-mongo-async-3.3:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-okhttp-2.2:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-okhttp-3.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-opentelemetry-annotations-1.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-opentelemetry-api-1.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-opentelemetry-api-metrics-1.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-rabbitmq-2.7:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-reactor-3.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-reactor-netty-0.9:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-reactor-netty-1.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-rxjava-2.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-rxjava-3.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-5.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-spring-integration-4.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-spring-rabbit-1.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-spring-scheduling-3.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-spring-webmvc-3.1:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-spring-webflux-5.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-tomcat-7.0:${otelInstrumentationAlphaVersion}")
  implementation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-tomcat-10.0:${otelInstrumentationAlphaVersion}")
}

// need to perform shading in two steps in order to avoid shading java.util.logging.Logger
// in opentelemetry-javaagent-java-util-logging-spans since that instrumentation needs to
// reference unshaded java.util.logging.Logger
// (java.util.logging.Logger shading is not needed in any of the instrumentation modules,
// but it is needed for the dependencies, e.g. guava, which use java.util.logging.Logger)
// -- AND ALSO --
// need to perform shading in two steps in order to avoid shading ch.qos.logback.*
// in opentelemetry-javaagent-logback-spans-1.0 since that instrumentation needs to
// reference unshaded ch.qos.logback.*
// (ch.qos.logback.* shading is not needed in any of the instrumentation modules,
// but it is needed for agent-tooling, which use logback to update levels dynamically in LazyConfigurationAccessor)
tasks {

  val shadowJarStep1 by registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {

    archiveClassifier.set("step1")
    destinationDirectory.set(file("${project.buildDir}/step1"))

    configurations.add(project.configurations.runtimeClasspath.get())

    dependencies {
      exclude(dependency("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-java-util-logging-spans"))
      exclude(dependency("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-logback-spans-1.0"))
    }

    // rewrite dependencies calling Logger.getLogger
    relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

    relocate("ch.qos.logback", "com.microsoft.applicationinsights.agent.shadow.ch.qos.logback")
  }

  shadowJar {

    dependsOn(shadowJarStep1)

    archiveClassifier.set("")

    from(zipTree(shadowJarStep1.get().archiveFile))

    mergeServiceFiles()

    exclude("**/module-info.class")

    // Prevents conflict with other SLF4J instances. Important for premain.
    relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")

    // rewrite library instrumentation dependencies
    relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation")

    // relocate OpenTelemetry API usage
    relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
    relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
    relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")

    // relocate the OpenTelemetry extensions that are used by instrumentation modules
    // these extensions live in the AgentClassLoader, and are injected into the user"s class loader
    // by the instrumentation modules that use them
    relocate("io.opentelemetry.extension.aws", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")
    relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")

    // this is for instrumentation on opentelemetry-api itself
    relocate("application.io.opentelemetry", "io.opentelemetry")

    // this is for instrumentation on java.util.logging itself
    relocate("application.java.util.logging.Logger", "java.util.logging.Logger")
  }
}
