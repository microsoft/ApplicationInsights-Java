[1mdiff --git a/agent/agent-tooling/src/main/java/com/microsoft/applicationinsights/agent/internal/init/SecondEntryPoint.java b/agent/agent-tooling/src/main/java/com/microsoft/applicationinsights/agent/internal/init/SecondEntryPoint.java[m
[1mindex 904414098f..34a1529649 100644[m
[1m--- a/agent/agent-tooling/src/main/java/com/microsoft/applicationinsights/agent/internal/init/SecondEntryPoint.java[m
[1m+++ b/agent/agent-tooling/src/main/java/com/microsoft/applicationinsights/agent/internal/init/SecondEntryPoint.java[m
[36m@@ -290,7 +290,7 @@[m [mpublic class SecondEntryPoint implements AutoConfigurationCustomizerProvider {[m
   private static TelemetryItemExporter initStatsbeatTelemetryItemExporter([m
       StatsbeatModule statsbeatModule, File tempDir, int diskPersistenceMaxSizeMb) {[m
     HttpPipeline httpPipeline = LazyHttpClient.newHttpPipeLine(null);[m
[31m-    TelemetryPipeline telemetryPipeline = new TelemetryPipeline(httpPipeline);[m
[32m+[m[32m    TelemetryPipeline telemetryPipeline = new TelemetryPipeline(httpPipeline, statsbeatModule);[m
 [m
     TelemetryPipelineListener telemetryPipelineListener;[m
     if (tempDir == null) {[m
[1mdiff --git a/agent/agent-tooling/src/main/java/com/microsoft/applicationinsights/agent/internal/telemetry/TelemetryClient.java b/agent/agent-tooling/src/main/java/com/microsoft/applicationinsights/agent/internal/telemetry/TelemetryClient.java[m
[1mindex 9c3f2acef3..a511ef432c 100644[m
[1m--- a/agent/agent-tooling/src/main/java/com/microsoft/applicationinsights/agent/internal/telemetry/TelemetryClient.java[m
[1m+++ b/agent/agent-tooling/src/main/java/com/microsoft/applicationinsights/agent/internal/telemetry/TelemetryClient.java[m
[36m@@ -34,10 +34,12 @@[m [mimport com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;[m
 import com.azure.monitor.opentelemetry.exporter.implementation.utils.TempDirs;[m
 import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;[m
 import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;[m
[32m+[m[32mimport io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;[m
 import io.opentelemetry.sdk.common.CompletableResultCode;[m
 import io.opentelemetry.sdk.resources.Resource;[m
 import java.io.File;[m
 import java.util.ArrayList;[m
[32m+[m[32mimport java.util.Collections;[m
 import java.util.HashMap;[m
 import java.util.List;[m
 import java.util.Map;[m
[36m@@ -227,7 +229,7 @@[m [mpublic class TelemetryClient {[m
         LazyHttpClient.newHttpPipeLine([m
             aadAuthentication,[m
             new NetworkStatsbeatHttpPipelinePolicy(statsbeatModule.getNetworkStatsbeat()));[m
[31m-    TelemetryPipeline telemetryPipeline = new TelemetryPipeline(httpPipeline);[m
[32m+[m[32m    TelemetryPipeline telemetryPipeline = new TelemetryPipeline(httpPipeline, statsbeatModule);[m
 [m
     TelemetryPipelineListener telemetryPipelineListener;[m
     if (tempDir == null) {[m
[36m@@ -335,7 +337,7 @@[m [mpublic class TelemetryClient {[m
       telemetryBuilder.addProperty(entry.getKey(), entry.getValue());[m
     }[m
     ResourceParser.updateRoleNameAndInstance([m
[31m-        telemetryBuilder, resource, com.azure.core.util.Configuration.getGlobalConfiguration());[m
[32m+[m[32m        telemetryBuilder, resource, DefaultConfigProperties.create(Collections.emptyMap()));[m
   }[m
 [m
   @Nullable[m
