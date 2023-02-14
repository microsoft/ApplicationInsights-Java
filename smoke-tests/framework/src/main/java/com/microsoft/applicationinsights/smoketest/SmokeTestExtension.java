// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Stopwatch;
import com.microsoft.applicationinsights.smoketest.fakeingestion.MockedAppInsightsIngestionServer;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import javax.annotation.Nullable;
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
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
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

  private static final String FAKE_INGESTION_ENDPOINT = "http://host.testcontainers.internal:6060/";
  private static final String FAKE_PROFILER_ENDPOINT =
      "http://host.testcontainers.internal:6060/profiler/";

  private static final File appFile = new File(System.getProperty("ai.smoke-test.test-app-file"));

  // TODO (trask) make private and expose methods on AiSmokeTest(?)
  protected final MockedAppInsightsIngestionServer mockedIngestion =
      new MockedAppInsightsIngestionServer();

  private boolean useAgent;
  @Nullable private String agentConfigurationPath;
  @Nullable private List<DependencyContainer> dependencyImages;

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
  private final String selfDiagnosticsLevel;
  private final File javaagentFile;
  private final File agentExtensionFile;

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
      boolean useOld3xAgent,
      String selfDiagnosticsLevel,
      File agentExtensionFile,
      ProfilerState profilerState) {
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
                + FAKE_INGESTION_ENDPOINT
                + ";LiveEndpoint="
                + FAKE_INGESTION_ENDPOINT
                + ";ProfilerEndpoint="
                + getProfilerEndpoint(profilerState);
    this.selfDiagnosticsLevel = selfDiagnosticsLevel;
    this.agentExtensionFile = agentExtensionFile;

    String javaagentPathSystemProperty =
        useOld3xAgent ? "ai.smoke-test.old-3x-javaagent-file" : "ai.smoke-test.javaagent-file";
    javaagentFile = new File(System.getProperty(javaagentPathSystemProperty));
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

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    if (targetContainer != null) {
      System.out.println("Test failure detected.");
      System.out.println("Container logs:");
      System.out.println(targetContainer.getLogs());
    }
  }

  private void prepareEnvironment(Environment environment) throws Exception {
    System.out.println("Preparing environment...");
    currentImageName = environment.value().getImageName();
    currentImageAppDir = environment.value().getImageAppDir();
    currentImageAppFileName = environment.value().getImageAppFileName();
    if (currentImageAppFileName == null) {
      currentImageAppFileName = appFile.getName();
    }
    mockedIngestion.startServer();
    network = Network.newNetwork();
    allContainers = new ArrayList<>();
    hostnameEnvVars = new HashMap<>();
    startDependencyContainers();
    startTestApplicationContainer();
    clearOutAnyInitLogs();
    mockedIngestion.setRequestLoggingEnabled(true);
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
      HttpHelper.get(url, targetUri.userAgent());
    }
  }

  protected String getBaseUrl() {
    String appContext = getAppContext();
    if (appContext.isEmpty()) {
      return "http://localhost:" + appServerPort;
    } else {
      return "http://localhost:" + appServerPort + "/" + appContext;
    }
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
      String contextRootUrl = getBaseUrl() + "/";
      HttpHelper.getResponseCodeEnsuringSampled(contextRootUrl);
      waitForHealthCheckTelemetry(contextRootUrl);
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
          .withStartupTimeout(Duration.ofSeconds(90));

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
              .withStartupTimeout(Duration.ofSeconds(90));
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

    GenericContainer<?> container;
    if (REMOTE_DEBUG) {
      container =
          new FixedHostPortGenericContainer<>(currentImageName)
              .withFixedExposedPort(5005, 5005)
              .waitingFor(
                  new HostPortWaitStrategy() {
                    @Override
                    protected Set<Integer> getLivenessCheckPorts() {
                      // this prevents ping to 5005 which causes the JVM to log a warning
                      // "Debugger failed to attach: handshake failed"
                      return singleton(8080);
                    }
                  }.withStartupTimeout(Duration.ofMinutes(5)));
    } else {
      container = new GenericContainer<>(currentImageName);
    }

    container =
        container
            .withEnv(hostnameEnvVars)
            .withEnv("APPLICATIONINSIGHTS_CONNECTION_STRING", connectionString)
            .withEnv("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL", selfDiagnosticsLevel)
            .withNetwork(network)
            .withExposedPorts(8080)
            .withFileSystemBind(
                appFile.getAbsolutePath(),
                currentImageAppDir + "/" + currentImageAppFileName,
                BindMode.READ_ONLY);

    List<String> javaToolOptions = new ArrayList<>();
    javaToolOptions.add("-Dapplicationinsights.testing.batch-schedule-delay-millis=500");
    javaToolOptions.add("-Dapplicationinsights.testing.metric-reader-interval-millis=500");
    if (agentExtensionFile != null) {
      javaToolOptions.add("-Dotel.javaagent.extensions=/" + agentExtensionFile.getName());
    }
    if (usesGlobalIngestionEndpoint) {
      javaToolOptions.add(
          "-Dapplicationinsights.testing.global-ingestion-endpoint=" + FAKE_INGESTION_ENDPOINT);
    }
    if (REMOTE_DEBUG) {
      javaToolOptions.add("-agentlib:jdwp=transport=dt_socket,address=*:5005,server=y,suspend=y");
    }
    if (useAgent) {
      javaToolOptions.add("-javaagent:/applicationinsights-agent.jar");
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
      container = container.withCommand("java -jar " + appFile.getName());
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
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  protected static <T extends Domain> T getBaseData(Envelope envelope) {
    return ((Data<T>) envelope.getData()).getBaseData();
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
}
