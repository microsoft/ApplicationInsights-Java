// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Stopwatch;
import com.microsoft.applicationinsights.smoketest.fakeingestion.MockedAppInsightsIngestionServer;
import com.microsoft.applicationinsights.smoketest.fakeingestion.MockedOtlpIngestionServer;
import com.microsoft.applicationinsights.smoketest.fakeingestion.ProfilerState;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Domain;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings({"SystemOut", "InterruptedExceptionSwallowed"})
public class SmokeTestExtension
    implements BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback,
        TestWatcher {

  // add -PsmokeTestRemoteDebug=true to the gradle args to enable (see ai.smoke-test.gradle.kts)
  private static final boolean REMOTE_DEBUG = Boolean.getBoolean("ai.smoke-test.remote-debug");

  private static final int TELEMETRY_RECEIVE_TIMEOUT_SECONDS = 60;

  private static final String FAKE_BREEZE_INGESTION_ENDPOINT =
      "http://host.testcontainers.internal:6060/";
  private static final String FAKE_OTLP_INGESTION_ENDPOINT =
      "http://host.testcontainers.internal:4318";
  private static final String FAKE_PROFILER_ENDPOINT =
      "http://host.testcontainers.internal:6060/profiler/";

  private static final File appFile = new File(System.getProperty("ai.smoke-test.test-app-file"));

  // TODO (trask) make private and expose methods on AiSmokeTest(?)
  protected final MockedAppInsightsIngestionServer mockedIngestion;

  protected final MockedOtlpIngestionServer mockedOtlpIngestion = new MockedOtlpIngestionServer();

  private boolean useAgent;
  @Nullable private String agentConfigurationPath;
  @Nullable private List<DependencyContainer> dependencyImages;
  @Nullable private EnvironmentValue currentEnvironment;

  @Nullable private String currentImageName;
  @Nullable private String currentImageAppDir;
  @Nullable private String currentImageAppFileName;

  @Nullable private GenericContainer<?> targetContainer;
  @Nullable private List<GenericContainer<?>> allContainers;
  @Nullable private Map<String, String> hostnameEnvVars;
  private int appServerPort;

  @Nullable private Network network;

  private final GenericContainer<?> dependencyContainer;
  private final String dependencyContainerEnvVarName;
  private final boolean skipHealthCheck;
  private final boolean readOnly;
  private final boolean usesGlobalIngestionEndpoint;
  private final boolean useOld3xAgent;
  private final String connectionString;
  private final String otelResourceAttributesEnvVar;
  private final String selfDiagnosticsLevel;
  private final File javaagentFile;
  private final File agentExtensionFile;
  private final Map<String, String> httpHeaders;
  private final Map<String, String> envVars;
  private final List<String> jvmArgs;
  private final boolean useDefaultHttpPort;
  private final boolean useOtlpEndpoint;

  public static SmokeTestExtension create() {
    return builder().build();
  }

  public static SmokeTestExtensionBuilder builder() {
    return new SmokeTestExtensionBuilder();
  }

  SmokeTestExtension(
      @Nullable GenericContainer<?> dependencyContainer,
      @Nullable String dependencyContainerEnvVarName,
      boolean usesGlobalIngestionEndpoint,
      boolean skipHealthCheck,
      boolean readOnly,
      boolean doNotSetConnectionString,
      String otelResourceAttributesEnvVar,
      boolean useOld3xAgent,
      String selfDiagnosticsLevel,
      File agentExtensionFile,
      ProfilerState profilerState,
      Map<String, String> httpHeaders,
      Map<String, String> envVars,
      List<String> jvmArgs,
      boolean useDefaultHttpPort,
      boolean useOtlpEndpoint) {
    this.skipHealthCheck = skipHealthCheck;
    this.readOnly = readOnly;
    this.dependencyContainer = dependencyContainer;
    this.dependencyContainerEnvVarName = dependencyContainerEnvVarName;
    this.usesGlobalIngestionEndpoint = usesGlobalIngestionEndpoint;
    this.useOld3xAgent = useOld3xAgent;
    connectionString =
        doNotSetConnectionString
            ? ""
            : "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF;IngestionEndpoint="
                + FAKE_BREEZE_INGESTION_ENDPOINT
                + ";LiveEndpoint="
                + FAKE_BREEZE_INGESTION_ENDPOINT
                + ";ProfilerEndpoint="
                + getProfilerEndpoint(profilerState);

    this.otelResourceAttributesEnvVar = otelResourceAttributesEnvVar;
    this.selfDiagnosticsLevel = selfDiagnosticsLevel;
    this.agentExtensionFile = agentExtensionFile;

    String javaagentPathSystemProperty =
        useOld3xAgent ? "ai.smoke-test.old-3x-javaagent-file" : "ai.smoke-test.javaagent-file";
    javaagentFile = new File(System.getProperty(javaagentPathSystemProperty));

    this.httpHeaders = httpHeaders;
    this.envVars = envVars;
    this.jvmArgs = jvmArgs;
    this.useDefaultHttpPort = useDefaultHttpPort;
    this.useOtlpEndpoint = useOtlpEndpoint;

    mockedIngestion = new MockedAppInsightsIngestionServer(useOld3xAgent);
  }

  private static String getProfilerEndpoint(ProfilerState profilerState) {
    return FAKE_PROFILER_ENDPOINT + profilerState.name() + "/";
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    try {
      beforeAllInternal(context);
    } catch (Exception e) {
      testFailed(context, e);
      throw e;
    }
  }

  private void beforeAllInternal(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();

    UseAgent ua = testClass.getAnnotation(UseAgent.class);
    if (ua != null) {
      useAgent = true;
      agentConfigurationPath = ua.value();
    } else {
      useAgent = false;
      agentConfigurationPath = null;
    }
    WithDependencyContainers wdc = testClass.getAnnotation(WithDependencyContainers.class);
    dependencyImages = new ArrayList<>();
    if (wdc != null) {
      Collections.addAll(dependencyImages, wdc.value());
    }

    Environment environment = testClass.getAnnotation(Environment.class);
    prepareEnvironment(environment);
  }

  private void prepareEnvironment(Environment environment) throws Exception {
    System.out.println("Preparing environment...");
    currentEnvironment = environment.value();
    currentImageName = environment.value().getImageName();
    currentImageAppDir = environment.value().getImageAppDir();
    currentImageAppFileName = environment.value().getImageAppFileName();
    if (currentImageAppFileName == null) {
      currentImageAppFileName = appFile.getName();
    }
    mockedIngestion.startServer();
    mockedIngestion.setRequestLoggingEnabled(true);
    mockedIngestion.setQuickPulseRequestLoggingEnabled(true);
    if (useOtlpEndpoint) {
      mockedOtlpIngestion.startServer();
    }
    network = Network.newNetwork();
    allContainers = new ArrayList<>();
    hostnameEnvVars = new HashMap<>();
    startDependencyContainers();
    startTestApplicationContainer();
    // TODO (trask) how to wait for startup in this case?
    if (useDefaultHttpPort) {
      Thread.sleep(15000);
    }
    clearOutAnyInitLogs();
  }

  public EnvironmentValue getCurrentEnvironment() {
    return currentEnvironment;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    System.out.println("Configuring test...");
    TargetUri targetUri = context.getRequiredTestMethod().getAnnotation(TargetUri.class);
    if (targetUri == null) {
      System.out.println("@TargetUri is missing, not making any server request");
      return;
    }

    String url = getBaseUrl() + targetUri.value();
    if (targetUri.callCount() == 1) {
      System.out.println("calling " + url);
    } else {
      System.out.println("calling " + url + " " + targetUri.callCount() + " times");
    }
    for (int i = 0; i < targetUri.callCount(); i++) {
      HttpHelper.get(url, targetUri.userAgent(), httpHeaders);
    }
  }

  protected String getBaseUrl() {
    String appContext = getAppContext();
    StringBuilder sb = new StringBuilder();
    sb.append("http://localhost");
    if (appServerPort != 80) {
      sb.append(':');
      sb.append(appServerPort);
    }
    if (!appContext.isEmpty()) {
      sb.append('/');
      sb.append(appContext);
    }
    return sb.toString();
  }

  protected String getAppContext() {
    if (currentImageAppFileName.endsWith(".jar")) {
      // spring boot jar
      return "";
    } else {
      return currentImageAppFileName.replace(".war", "");
    }
  }

  private void clearOutAnyInitLogs() throws Exception {
    if (!skipHealthCheck) {
      if (!useOld3xAgent) {
        await().until(mockedIngestion::isReceivingLiveMetrics);
      }
      String contextRootUrl = getBaseUrl() + "/";
      HttpHelper.getResponseCodeEnsuringSampled(contextRootUrl);
      waitForHealthCheckTelemetry(contextRootUrl);
      if (!useOld3xAgent) {
        try {
          await()
              .untilAsserted(
                  () ->
                      assertThat(mockedIngestion.getLiveMetrics().getRequestCount(contextRootUrl))
                          .isEqualTo(1));
        } catch (ConditionTimeoutException e) {
          // TODO (trask) need to fix race condition in live metrics
          //  where sometimes it loses telemetry
        }
      }
      System.out.println("Clearing any RequestData from health check.");
      mockedIngestion.resetData();
    }
  }

  private void waitForHealthCheckTelemetry(String contextRootUrl)
      throws InterruptedException, ExecutionException, TimeoutException {
    Stopwatch receivedTelemetryTimer = Stopwatch.createStarted();
    try {
      mockedIngestion.waitForItem(
          input -> {
            if (!"RequestData".equals(input.getData().getBaseType())) {
              return false;
            }
            RequestData data = (RequestData) ((Data<?>) input.getData()).getBaseData();
            return contextRootUrl.equals(data.getUrl()) && "200".equals(data.getResponseCode());
          },
          TELEMETRY_RECEIVE_TIMEOUT_SECONDS,
          TimeUnit.SECONDS);
      if (!useOld3xAgent) {
        mockedIngestion.waitForItem(
            input -> {
              if (!"MetricData".equals(input.getData().getBaseType())) {
                return false;
              }
              MetricData data = (MetricData) ((Data<?>) input.getData()).getBaseData();
              String metricId = data.getProperties().get("_MS.MetricId");
              return metricId != null && metricId.equals("requests/duration");
            },
            10, // metrics should come in pretty quickly after spans
            TimeUnit.SECONDS);
      }
      System.out.printf(
          "Received request telemetry after %.3f seconds...%n",
          receivedTelemetryTimer.elapsed(TimeUnit.MILLISECONDS) / 1000.0);
    } catch (TimeoutException e) {
      TimeoutException withMessage =
          new TimeoutException("request telemetry from application health check");
      withMessage.initCause(e);
      throw withMessage;
    }
  }

  private void startDependencyContainers() {
    if (dependencyContainer != null) {
      System.out.println("Starting container: " + dependencyContainer.getDockerImageName());
      String containerName = "dependency" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
      dependencyContainer
          .withNetwork(network)
          .withNetworkAliases(containerName)
          .withStartupTimeout(Duration.ofMinutes(5));

      Stopwatch stopwatch = Stopwatch.createStarted();
      dependencyContainer.start();
      System.out.printf(
          "Dependency container %s (%s) started after %.3f seconds%n",
          dependencyContainer.getDockerImageName(),
          dependencyContainer.getContainerId(),
          stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0);

      if (dependencyContainerEnvVarName != null) {
        hostnameEnvVars.put(dependencyContainerEnvVarName, containerName);
      }
      allContainers.add(dependencyContainer);
    }
    for (DependencyContainer dc : dependencyImages) {
      String imageName = dc.imageName().isEmpty() ? dc.value() : dc.imageName();
      System.out.println("Starting container: " + imageName);
      String containerName = "dependency" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
      String[] envVars = substitue(dc.environmentVariables());
      Map<String, String> envVarMap = new HashMap<>();
      for (String envVar : envVars) {
        String[] parts = envVar.split("=");
        envVarMap.put(parts[0], parts[1]);
      }
      GenericContainer<?> container =
          new GenericContainer<>(DockerImageName.parse(imageName))
              .withEnv(envVarMap)
              .withNetwork(network)
              .withNetworkAliases(containerName)
              .withExposedPorts(dc.exposedPort())
              .withStartupTimeout(Duration.ofMinutes(5));
      Stopwatch stopwatch = Stopwatch.createStarted();
      container.start();
      System.out.printf(
          "Dependency container %s (%s) started after %.3f seconds%n",
          imageName, container.getContainerId(), stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0);

      if (!dc.hostnameEnvironmentVariable().isEmpty()) {
        hostnameEnvVars.put(dc.hostnameEnvironmentVariable(), containerName);
      }
      allContainers.add(container);
    }
  }

  private String[] substitue(String[] environmentVariables) {
    String[] envVars = new String[environmentVariables.length];
    for (int i = 0; i < environmentVariables.length; i++) {
      envVars[i] = substitute(environmentVariables[i]);
    }
    return envVars;
  }

  private String substitute(String environmentVariable) {
    String envVar = environmentVariable;
    for (Map.Entry<String, String> entry : hostnameEnvVars.entrySet()) {
      envVar = envVar.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    return envVar;
  }

  @SuppressWarnings(
      "deprecation") // intentionally using FixedHostPortGenericContainer when remote debugging
  // enabled
  private void startTestApplicationContainer() throws Exception {
    System.out.println("Starting app container...");

    // TODO (trask) make this port dynamic so can run tests in parallel
    Testcontainers.exposeHostPorts(6060);
    Testcontainers.exposeHostPorts(4318);

    GenericContainer<?> container;
    if (REMOTE_DEBUG || useDefaultHttpPort) {
      FixedHostPortGenericContainer fixedPortContainer =
          new FixedHostPortGenericContainer<>(currentImageName);
      if (REMOTE_DEBUG) {
        fixedPortContainer
            .withFixedExposedPort(5005, 5005)
            .withStartupTimeout(Duration.ofMinutes(5));
      }
      if (useDefaultHttpPort) {
        fixedPortContainer.withFixedExposedPort(80, 8080);
      } else {
        fixedPortContainer.withExposedPorts(8080);
      }
      container = fixedPortContainer;
    } else {
      container = new GenericContainer<>(currentImageName).withExposedPorts(8080);
    }

    container =
        container
            .withEnv(hostnameEnvVars)
            .withEnv("APPLICATIONINSIGHTS_CONNECTION_STRING", connectionString)
            .withEnv("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL", selfDiagnosticsLevel)
            .withEnv("OTEL_RESOURCE_ATTRIBUTES", otelResourceAttributesEnvVar)
            .withEnv("APPLICATIONINSIGHTS_METRIC_INTERVAL_SECONDS", "1")
            .withEnv(envVars)
            .withNetwork(network)
            .withFileSystemBind(
                appFile.getAbsolutePath(),
                currentImageAppDir + "/" + currentImageAppFileName,
                BindMode.READ_ONLY);

    List<String> javaToolOptions = new ArrayList<>();
    javaToolOptions.add("-Dapplicationinsights.testing.batch-schedule-delay-millis=500");
    if (agentExtensionFile != null) {
      javaToolOptions.add("-Dotel.javaagent.extensions=/" + agentExtensionFile.getName());
    }
    if (usesGlobalIngestionEndpoint) {
      javaToolOptions.add(
          "-Dapplicationinsights.testing.global-ingestion-endpoint="
              + FAKE_BREEZE_INGESTION_ENDPOINT);
    }
    if (useOtlpEndpoint) {
      // TODO (trask) don't use azure_monitor exporter for smoke test health check
      javaToolOptions.add("-Dotel.metrics.exporter=otlp,azure_monitor");
      javaToolOptions.add("-Dotel.exporter.otlp.metrics.endpoint=" + FAKE_OTLP_INGESTION_ENDPOINT);
      javaToolOptions.add("-Dotel.exporter.otlp.protocol=http/protobuf");
    }
    if (REMOTE_DEBUG) {
      javaToolOptions.add(
          "-agentlib:jdwp=transport=dt_socket,address=0.0.0.0:5005,server=y,suspend=y");
    }
    if (useAgent) {
      javaToolOptions.add("-javaagent:/applicationinsights-agent.jar");
      javaToolOptions.add(
          "-Dapplicationinsights.testing.statsbeat.ikey=00000000-0000-0000-0000-0FEEDDADBEEG");
      javaToolOptions.add(
          "-Dapplicationinsights.testing.statsbeat.endpoint=http://host.testcontainers.internal:6060/");
    }
    container.withEnv("JAVA_TOOL_OPTIONS", String.join(" ", javaToolOptions));

    container = addAdditionalFile(container);

    if (useAgent) {
      container =
          container.withFileSystemBind(
              javaagentFile.getAbsolutePath(),
              "/applicationinsights-agent.jar",
              BindMode.READ_ONLY);
      URL resource = SmokeTestExtension.class.getClassLoader().getResource(agentConfigurationPath);
      if (resource == null) {
        resource =
            SmokeTestExtension.class
                .getClassLoader()
                .getResource("default_applicationinsights.json");
      }
      File json = File.createTempFile("applicationinsights", ".json");
      Path jsonPath = json.toPath();
      try (InputStream in = resource.openStream()) {
        Files.copy(in, jsonPath, StandardCopyOption.REPLACE_EXISTING);
      }
      container =
          container.withFileSystemBind(
              json.getAbsolutePath(), "/applicationinsights.json", BindMode.READ_ONLY);
    }

    if (appFile.getName().endsWith(".jar")) {
      List<String> parts = new ArrayList<>();
      parts.add("java");
      parts.addAll(jvmArgs);
      parts.add("-jar");
      parts.add(appFile.getName());
      container = container.withCommand(parts.toArray(new String[0]));
    }

    if (readOnly) {
      container.withCreateContainerCmdModifier(
          createContainerCmd -> createContainerCmd.getHostConfig().withReadonlyRootfs(true));
    }

    Stopwatch stopwatch = Stopwatch.createStarted();
    container.start();
    System.out.printf(
        "App container (%s) started after %.3f seconds%n",
        container.getContainerId(), stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0);

    appServerPort = container.getMappedPort(8080);

    targetContainer = container;
    allContainers.add(container);
  }

  private GenericContainer<?> addAdditionalFile(GenericContainer<?> container) {
    if (agentExtensionFile != null) {
      return container.withFileSystemBind(
          agentExtensionFile.getAbsolutePath(),
          "/" + agentExtensionFile.getName(),
          BindMode.READ_ONLY);
    }
    return container;
  }

  @Override
  public void afterEach(ExtensionContext context) {
    mockedIngestion.resetData();
    System.out.println("Mocked ingestion reset.");
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (allContainers != null) {
      if (targetContainer != null) {
        System.out.println("Test failure detected.");
        System.out.println("Container logs:");
        System.out.println(targetContainer.getLogs());
      }
      System.out.println("Stopping containers...");
      for (GenericContainer<?> container : allContainers) {
        container.stop();
      }
    }
    if (network != null) {
      network.close();
    }

    mockedIngestion.stopServer();
    mockedIngestion.setRequestLoggingEnabled(false);
    mockedIngestion.setQuickPulseRequestLoggingEnabled(false);
    if (useOtlpEndpoint) {
      mockedOtlpIngestion.stopServer();
    }
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  protected static <T extends Domain> T getBaseData(Envelope envelope) {
    @SuppressWarnings("unchecked")
    Data<T> data = (Data<T>) envelope.getData();
    return data.getBaseData();
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  protected <T extends Domain> T getTelemetryDataForType(int index, String type) {
    return mockedIngestion.getBaseDataForType(index, type);
  }

  protected Telemetry getTelemetry(int rddCount) throws Exception {
    return getTelemetry(rddCount, rdd -> true);
  }

  protected Telemetry getTelemetry(int rddCount, Predicate<RemoteDependencyData> condition)
      throws Exception {

    if (rddCount > 3) {
      throw new IllegalArgumentException("this method currently only supports rddCount up to 3");
    }

    Telemetry telemetry = new Telemetry();

    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    telemetry.rdEnvelope = rdList.get(0);
    telemetry.rd = (RequestData) ((Data<?>) telemetry.rdEnvelope.getData()).getBaseData();

    assertEquals(0, mockedIngestion.getCountForType("EventData"));

    if (rddCount == 0) {
      return telemetry;
    }

    String operationId = telemetry.rdEnvelope.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        mockedIngestion.waitForItemsInOperation(
            "RemoteDependencyData",
            rddCount,
            operationId,
            envelope -> {
              RemoteDependencyData rdd =
                  (RemoteDependencyData) ((Data<?>) envelope.getData()).getBaseData();
              return condition.test(rdd);
            });

    telemetry.rddEnvelope1 = rddList.get(0);
    telemetry.rdd1 =
        (RemoteDependencyData) ((Data<?>) telemetry.rddEnvelope1.getData()).getBaseData();

    if (rddCount == 1) {
      return telemetry;
    }

    telemetry.rddEnvelope2 = rddList.get(1);
    telemetry.rdd2 =
        (RemoteDependencyData) ((Data<?>) telemetry.rddEnvelope2.getData()).getBaseData();

    if (rddCount == 2) {
      return telemetry;
    }

    telemetry.rddEnvelope3 = rddList.get(2);
    telemetry.rdd3 =
        (RemoteDependencyData) ((Data<?>) telemetry.rddEnvelope3.getData()).getBaseData();

    return telemetry;
  }

  public static void assertParentChild(
      RequestData rd, Envelope parentEnvelope, Envelope childEnvelope, String operationName) {
    assertParentChild(
        rd.getId(), parentEnvelope, childEnvelope, operationName, operationName, true);
  }

  public static void assertParentChild(
      RemoteDependencyData rdd,
      Envelope parentEnvelope,
      Envelope childEnvelope,
      String operationName) {
    assertParentChild(
        rdd.getId(), parentEnvelope, childEnvelope, operationName, operationName, false);
  }

  public static void assertParentChild(
      String parentId,
      Envelope parentEnvelope,
      Envelope childEnvelope,
      String parentOperationName,
      String childOperationName,
      boolean topLevelParent) {
    String operationId = parentEnvelope.getTags().get("ai.operation.id");
    assertThat(operationId).isNotNull();
    assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));

    String operationParentId = parentEnvelope.getTags().get("ai.operation.parentId");
    if (topLevelParent) {
      assertThat(operationParentId).isNull();
    } else {
      assertThat(operationParentId).isNotNull();
    }

    assertEquals(parentId, childEnvelope.getTags().get("ai.operation.parentId"));

    assertEquals(parentOperationName, parentEnvelope.getTags().get("ai.operation.name"));
    assertEquals(childOperationName, childEnvelope.getTags().get("ai.operation.name"));
  }

  public static Predicate<Envelope> getMetricPredicate(String name) {
    Objects.requireNonNull(name, "name");
    return input -> {
      if (input == null) {
        return false;
      }
      if (!input.getData().getBaseType().equals("MetricData")) {
        return false;
      }
      MetricData md = getBaseData(input);
      return name.equals(md.getMetrics().get(0).getName());
    };
  }

  public static Predicate<Envelope> getMetricPredicate(
      String name, String secondPredicate, boolean isRolename) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(secondPredicate, "secondPredicate");

    return input -> {
      if (input == null) {
        return false;
      }
      if (!input.getData().getBaseType().equals("MetricData")
          || (isRolename && !input.getTags().get("ai.cloud.role").equals(secondPredicate))
          || (!isRolename && !input.getIKey().equals(secondPredicate))) {
        return false;
      }
      MetricData md = getBaseData(input);
      boolean isSecondPredicateValid =
          isRolename
              ? secondPredicate.equals(input.getTags().get("ai.cloud.role"))
              : secondPredicate.equals(input.getIKey());
      return name.equals(md.getMetrics().get(0).getName()) && isSecondPredicateValid;
    };
  }

  public static Predicate<Envelope> getStandardMetricPredicate(String metricId) {
    Objects.requireNonNull(metricId, "metricId");
    return input -> {
      if (input == null) {
        return false;
      }
      if (!input.getData().getBaseType().equals("MetricData")) {
        return false;
      }
      MetricData md = getBaseData(input);
      return metricId.equals(md.getProperties().get("_MS.MetricId"));
    };
  }

  public void waitAndAssertTrace(Consumer<TraceAssert> assertions) {
    await()
        .untilAsserted(
            () -> {
              List<Envelope> envelopes = mockedIngestion.getAllItems();

              Collection<List<Envelope>> traces =
                  envelopes.stream()
                      .filter(envelope -> envelope.getTags().get("ai.operation.id") != null)
                      .collect(
                          Collectors.groupingBy(
                              envelope -> envelope.getTags().get("ai.operation.id")))
                      .values();

              assertThat(traces).anySatisfy(trace -> assertions.accept(new TraceAssert(trace)));
            });
  }

  // passing name into this, instead of asserting the name afterwards via hasName()
  // because otherwise the failure messages are huge (showing why every single metric captured
  // didn't match)
  public void waitAndAssertMetric(String name, Consumer<MetricAssert> assertions) {
    // TODO assert that metrics are never sent with sample rate
    //  assertThat(mdEnvelope.getSampleRate()).isNull();

    // TODO
    //  assertThat(metrics.size()).isEqualTo(1);
    await()
        .untilAsserted(
            () -> {
              List<Envelope> envelopes =
                  mockedIngestion.getItemsEnvelopeDataType("MetricData").stream()
                      .filter(getMetricPredicate(name))
                      .collect(Collectors.toList());
              assertThat(envelopes)
                  .anySatisfy(envelope -> assertions.accept(new MetricAssert(envelope)));
            });
  }
}
