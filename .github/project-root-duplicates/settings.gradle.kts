import org.apache.tools.ant.taskdefs.condition.Os

pluginManagement {
  plugins {
    id("com.github.ben-manes.versions") version "0.43.0"
    id("com.github.jk1.dependency-license-report") version "2.1"
    id("me.champeau.jmh") version "0.6.8"
    id("com.gradle.plugin-publish") version "1.0.0"
  }
}

plugins {
  id("com.gradle.enterprise") version "3.11.4"
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
  }
}

val isCI = System.getenv("CI") != null
gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    if (isCI) {
      publishAlways()
      tag("CI")
    }
  }
}

rootProject.name = "applicationinsights-java"

val buildNative = System.getProperty("ai.etw.native.build") != null && Os.isFamily(Os.FAMILY_WINDOWS)
if (buildNative) {
  include(":etw:native")
} else {
  logger.info("Skipping build of :etw:native. EtwAppender/EtwProvider will not work because library is missing")
}
include(":etw:java")
// TODO (trask) ETW: is this really needed? if so, need to restore devtest configuration
// include(":etw:etw-testapp")

include(":agent:agent-gc-monitor:gc-monitor-api")
include(":agent:agent-gc-monitor:gc-monitor-core")
include(":agent:agent-gc-monitor:gc-monitor-tests")

include(":agent:agent-profiler:agent-alerting-api")
include(":agent:agent-profiler:agent-diagnostics-api")
include(":agent:agent-profiler:agent-alerting")

include(":agent:agent-bootstrap")
include(":agent:agent-tooling")
include(":agent:azure-monitor-exporter")
include(":agent:agent-for-testing")
include(":agent:instrumentation:applicationinsights-web-2.3")
include(":agent:instrumentation:azure-functions")
include(":agent:instrumentation:methods")
include(":agent:instrumentation:micrometer-1.0")
include(":agent:agent")
include(":agent:runtime-attach")

include(":classic-sdk:core")
include(":classic-sdk:web")

// misc
include(":dependencyManagement")

include(":smoke-tests:framework")

include(":smoke-tests:apps:ActuatorMetrics")
include(":smoke-tests:apps:AutoPerfCounters")
include(":smoke-tests:apps:AzureSdk")
include(":smoke-tests:apps:Cassandra")
include(":smoke-tests:apps:ClassicSdkWebInterop2x")
include(":smoke-tests:apps:ClassicSdkWebInterop3x")
include(":smoke-tests:apps:ClassicSdkWebInterop3xUsingOld3xAgent")
include(":smoke-tests:apps:ConnectionStringOverrides")
include(":smoke-tests:apps:CoreAndFilter2x")
include(":smoke-tests:apps:CoreAndFilter3x")
include(":smoke-tests:apps:CoreAndFilter3xUsingOld3xAgent")
include(":smoke-tests:apps:CustomDimensions")
include(":smoke-tests:apps:CustomInstrumentation")
include(":smoke-tests:apps:gRPC")
include(":smoke-tests:apps:HeartBeat")
include(":smoke-tests:apps:HttpClients")
include(":smoke-tests:apps:HttpHeaders")
include(":smoke-tests:apps:HttpPreaggregatedMetrics")
include(":smoke-tests:apps:HttpServer4xx")
include(":smoke-tests:apps:InheritedAttributes")
include(":smoke-tests:apps:InstrumentationKeyOverrides")
include(":smoke-tests:apps:Jdbc")
include(":smoke-tests:apps:Jedis")
include(":smoke-tests:apps:JettyNativeHandler")
include(":smoke-tests:apps:JMS")
include(":smoke-tests:apps:Kafka")
include(":smoke-tests:apps:Lettuce")
include(":smoke-tests:apps:Micrometer")
include(":smoke-tests:apps:MongoDB")
include(":smoke-tests:apps:NonDaemonThreads")
include(":smoke-tests:apps:OpenTelemetryApiSupport")
include(":smoke-tests:apps:OpenTelemetryMetric")
include(":smoke-tests:apps:PreAggMetricsWithRoleNameOverridesAndSampling")
include(":smoke-tests:apps:RateLimitedSampling")
include(":smoke-tests:apps:ReadOnly")
include(":smoke-tests:apps:RoleNameOverrides")
include(":smoke-tests:apps:Sampling")
include(":smoke-tests:apps:SamplingOverrides")
include(":smoke-tests:apps:SamplingOverridesBackCompat")
include(":smoke-tests:apps:SpringBoot")
include(":smoke-tests:apps:SpringBootAttachInMain")
include(":smoke-tests:apps:SpringBootAuto")
include(":smoke-tests:apps:SpringBootAuto1_3")
include(":smoke-tests:apps:SpringCloudStream")
include(":smoke-tests:apps:SpringScheduling")
include(":smoke-tests:apps:Statsbeat")
include(":smoke-tests:apps:SystemExit")
include(":smoke-tests:apps:TelemetryProcessors")
include(":smoke-tests:apps:TraceJavaUtilLoggingUsingAgent")
include(":smoke-tests:apps:TraceLog4j1_2")
include(":smoke-tests:apps:TraceLog4j1_2UsingAgent")
include(":smoke-tests:apps:TraceLog4j2")
include(":smoke-tests:apps:TraceLog4j2UsingAgent")
include(":smoke-tests:apps:TraceLogBack")
include(":smoke-tests:apps:TraceLogBackUsingAgent")
include(":smoke-tests:apps:VerifyShading")
include(":smoke-tests:apps:WebAuto")
include(":smoke-tests:apps:WebFlux")
