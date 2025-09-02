import org.apache.tools.ant.taskdefs.condition.Os

pluginManagement {
  plugins {
    id("com.github.jk1.dependency-license-report") version "2.9"
    id("me.champeau.jmh") version "0.7.3"
    id("com.gradle.plugin-publish") version "1.3.1"
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
  }
}

rootProject.name = "ApplicationInsights-Java"

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

include(":agent:agent-profiler:agent-diagnostics-jfr")
include(":agent:agent-profiler:agent-alerting-api")
include(":agent:agent-profiler:agent-diagnostics-api")
include(":agent:agent-profiler:agent-diagnostics")
include(":agent:agent-profiler:agent-alerting")

include(":agent:agent-bootstrap")
include(":agent:agent-tooling")
include(":agent:agent-for-testing")
hideFromDependabot(":agent:instrumentation:applicationinsights-web-2.3")
include(":agent:instrumentation:azure-functions")
include(":agent:instrumentation:azure-functions-worker-stub")
include(":agent:instrumentation:methods")
hideFromDependabot(":agent:instrumentation:micrometer-1.0")
include(":agent:agent")
include(":agent:runtime-attach")

include(":classic-sdk:core")
include(":classic-sdk:web")

// misc
include(":dependencyManagement")

include(":smoke-tests:framework")

// TODO (trask) consider un-hiding these and running smoke tests against the latest versions
hideFromDependabot(":smoke-tests:apps:ActuatorMetrics")
hideFromDependabot(":smoke-tests:apps:AutoPerfCounters")
hideFromDependabot(":smoke-tests:apps:AzureSdk")
hideFromDependabot(":smoke-tests:apps:AzureFunctions")
hideFromDependabot(":smoke-tests:apps:BrowserSdkLoader")
hideFromDependabot(":smoke-tests:apps:Cassandra")
hideFromDependabot(":smoke-tests:apps:ClassicSdkLog4j1Interop2x")
hideFromDependabot(":smoke-tests:apps:ClassicSdkLog4j2Interop2x")
hideFromDependabot(":smoke-tests:apps:ClassicSdkLogbackInterop2x")
hideFromDependabot(":smoke-tests:apps:ClassicSdkWebInterop2x")
hideFromDependabot(":smoke-tests:apps:ClassicSdkWebInterop3x")
hideFromDependabot(":smoke-tests:apps:ClassicSdkWebInterop3xUsingOld3xAgent")
hideFromDependabot(":smoke-tests:apps:ConnectionStringOverrides")
hideFromDependabot(":smoke-tests:apps:CoreAndFilter2x")
hideFromDependabot(":smoke-tests:apps:CoreAndFilter3x")
hideFromDependabot(":smoke-tests:apps:CoreAndFilter3xUsingOld3xAgent")
hideFromDependabot(":smoke-tests:apps:CustomDimensions")
hideFromDependabot(":smoke-tests:apps:CustomInstrumentation")
hideFromDependabot(":smoke-tests:apps:DetectUnexpectedOtelMetrics")
hideFromDependabot(":smoke-tests:apps:Diagnostics")
hideFromDependabot(":smoke-tests:apps:Diagnostics:JfrFileReader")
hideFromDependabot(":smoke-tests:apps:DiagnosticExtension:MockExtension")
hideFromDependabot(":smoke-tests:apps:DiagnosticExtension")
hideFromDependabot(":smoke-tests:apps:JmxMetric")
hideFromDependabot(":smoke-tests:apps:gRPC")
hideFromDependabot(":smoke-tests:apps:HeartBeat")
hideFromDependabot(":smoke-tests:apps:HttpClients")
hideFromDependabot(":smoke-tests:apps:HttpHeaders")
hideFromDependabot(":smoke-tests:apps:HttpPreaggregatedMetrics")
hideFromDependabot(":smoke-tests:apps:HttpServer4xx")
hideFromDependabot(":smoke-tests:apps:HttpServer")
hideFromDependabot(":smoke-tests:apps:InheritedAttributes")
hideFromDependabot(":smoke-tests:apps:InstrumentationKeyOverrides")
hideFromDependabot(":smoke-tests:apps:JavaProfiler")
hideFromDependabot(":smoke-tests:apps:JavaUtilLogging")
hideFromDependabot(":smoke-tests:apps:Jdbc")
hideFromDependabot(":smoke-tests:apps:Jedis")
hideFromDependabot(":smoke-tests:apps:JettyNativeHandler")
hideFromDependabot(":smoke-tests:apps:JMS")
hideFromDependabot(":smoke-tests:apps:Kafka")
hideFromDependabot(":smoke-tests:apps:Lettuce")
hideFromDependabot(":smoke-tests:apps:LiveMetrics")
hideFromDependabot(":smoke-tests:apps:Log4j1")
hideFromDependabot(":smoke-tests:apps:Log4j2")
hideFromDependabot(":smoke-tests:apps:Logback")
hideFromDependabot(":smoke-tests:apps:Micrometer")
hideFromDependabot(":smoke-tests:apps:MongoDB")
hideFromDependabot(":smoke-tests:apps:NonDaemonThreads")
hideFromDependabot(":smoke-tests:apps:OpenTelemetryApiSupport")
hideFromDependabot(":smoke-tests:apps:OpenTelemetryApiLogBridge")
hideFromDependabot(":smoke-tests:apps:OpenTelemetryMetric")
hideFromDependabot(":smoke-tests:apps:OtelResourceCustomMetric")
hideFromDependabot(":smoke-tests:apps:OtlpMetrics")
hideFromDependabot(":smoke-tests:apps:OutOfMemoryWithDebugLevel")
hideFromDependabot(":smoke-tests:apps:PreAggMetricsWithRoleNameOverridesAndSampling")
hideFromDependabot(":smoke-tests:apps:PreferForwardedUrlScheme")
hideFromDependabot(":smoke-tests:apps:RateLimitedSampling")
hideFromDependabot(":smoke-tests:apps:ReadOnly")
hideFromDependabot(":smoke-tests:apps:RoleNameOverrides")
hideFromDependabot(":smoke-tests:apps:RuntimeAttach")
hideFromDependabot(":smoke-tests:apps:RuntimeAttachWithDelayedConnectionString")
hideFromDependabot(":smoke-tests:apps:Sampling")
hideFromDependabot(":smoke-tests:apps:SamplingOverrides")
hideFromDependabot(":smoke-tests:apps:SamplingOverridesBackCompat")
hideFromDependabot(":smoke-tests:apps:SpringBoot")
hideFromDependabot(":smoke-tests:apps:SpringBootAuto")
hideFromDependabot(":smoke-tests:apps:SpringBootAuto1_3")
hideFromDependabot(":smoke-tests:apps:SpringCloudStream")
hideFromDependabot(":smoke-tests:apps:SpringScheduling")
hideFromDependabot(":smoke-tests:apps:Statsbeat")
hideFromDependabot(":smoke-tests:apps:SystemExit")
hideFromDependabot(":smoke-tests:apps:TelemetryProcessors")
hideFromDependabot(":smoke-tests:apps:VerifyAgentJar")
hideFromDependabot(":smoke-tests:apps:WebAuto")
hideFromDependabot(":smoke-tests:apps:WebFlux")

// this effectively hides the submodule from dependabot because dependabot only regex parses gradle
// files looking for certain patterns
fun hideFromDependabot(projectPath: String) {
  include(projectPath)
}
