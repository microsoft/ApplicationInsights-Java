/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Stopwatch;
import com.microsoft.applicationinsights.smoketest.fakeingestion.MockedAppInsightsIngestionServer;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SuppressWarnings({"SystemOut", "InterruptedExceptionSwallowed"})
public class AiSmokeTest
    implements BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback,
        TestWatcher {

  // use -PsmokeTestRemoteDebug=true at gradle command line to enable (see ai.smoke-test.gradle.kts)
  private static final boolean REMOTE_DEBUG = Boolean.getBoolean("ai.smoke-test.remote-debug");

  private static final int TELEMETRY_RECEIVE_TIMEOUT_SECONDS = 60;

  private static File appFile;
  private static File javaagentFile;

  private final List<DependencyContainer> dependencyImages = new ArrayList<>();
  private final AtomicReference<GenericContainer<?>> targetContainer = new AtomicReference<>();
  private final Deque<GenericContainer<?>> allContainers = new ArrayDeque<>();
  private final Map<String, String> hostnameEnvVars = new HashMap<>();
  protected String currentImageName;
  private String currentImageAppDir;
  private int appServerPort;

  private boolean useAgent;
  @Nullable private String agentConfigurationPath;
  @Nullable private Network network;

  protected final MockedAppInsightsIngestionServer mockedIngestion =
      new MockedAppInsightsIngestionServer();

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    try {
      beforeAllInternal(context);
    } catch (Exception e) {
      testFailed(context, e);
    }
  }

  private void beforeAllInternal(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();

    Environment environment = testClass.getAnnotation(Environment.class);
    String imageName = environment.value().getImageName();
    String imageAppDir = environment.value().getImageAppDir();

    UseAgent ua = testClass.getAnnotation(UseAgent.class);
    if (ua != null) {
      useAgent = true;
      agentConfigurationPath = ua.value();
    }
    WithDependencyContainers wdc = testClass.getAnnotation(WithDependencyContainers.class);
    if (wdc != null) {
      Collections.addAll(dependencyImages, wdc.value());
    }

    configureEnvironment(imageName, imageAppDir);
  }

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    System.out.println("Test failure detected.");
    System.out.println("Container logs:");
    System.out.println(targetContainer.get().getLogs());
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    System.out.println("Configuring test...");
    TargetUri targetUri = context.getRequiredTestMethod().getAnnotation(TargetUri.class);
    if (targetUri == null) {
      System.out.println("targetUri==null: automated testapp request disabled");
      return;
    }

    System.out.println("Calling " + targetUri.value() + " ...");
    String url = getBaseUrl() + targetUri.value();
    if (targetUri.callCount() == 1) {
      System.out.println("calling " + url);
    } else {
      System.out.println("calling " + url + " " + targetUri.callCount() + " times");
    }
    for (int i = 0; i < targetUri.callCount(); i++) {
      String content = HttpHelper.get(url);
      String expectationMessage = "The base context in testApps should return a nonempty response.";
      assertNotNull(
          String.format(
              "Null response from targetUri: '%s'. %s", targetUri.value(), expectationMessage),
          content);
      assertTrue(
          String.format(
              "Empty response from targetUri: '%s'. %s", targetUri.value(), expectationMessage),
          content.length() > 0);
    }
    Stopwatch sw = Stopwatch.createStarted();
    mockedIngestion.awaitAnyItems(TELEMETRY_RECEIVE_TIMEOUT_SECONDS * 1000, TimeUnit.MILLISECONDS);
    System.out.printf(
        "Telemetry received after %.3f seconds.%n", sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0);
    System.out.println("Starting validation...");
    assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
  }

  public void configureEnvironment(String imageName, String imageAppDir) throws Exception {
    System.out.println("Preparing environment...");

    GenericContainer<?> containerInfo = targetContainer.get();
    if (containerInfo != null) {
      // test cleanup didn't take...try to clean up
      if (containerInfo.isRunning()) {
        System.err.println("From last test run, container is still running: " + containerInfo);
        try {
          containerInfo.stop();
        } catch (RuntimeException e) {
          System.err.println("Couldn't clean up environment. Must be done manually.");
          throw e;
        }
      } else {
        // container must have stopped after timeout reached.
        targetContainer.set(null);
      }
    }
    setupProperties(imageName, imageAppDir);
    startMockedIngestion();
    createDockerNetwork();
    startAllContainers();
    clearOutAnyInitLogs();
    mockedIngestion.setRequestLoggingEnabled(true);
    System.out.println("Environment preparation complete.");
  }

  protected static String getAppContext() {
    String appFileName = appFile.getName();
    if (appFileName.endsWith(".jar")) {
      // spring boot jar
      return "";
    } else {
      return appFileName.replace(".war", "");
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

  private void clearOutAnyInitLogs() throws Exception {
    String contextRootUrl = getBaseUrl() + "/";
    HttpHelper.getResponseCodeEnsuringSampled(contextRootUrl);
    waitForHealthCheckTelemetryIfNeeded(contextRootUrl);
    System.out.println("Clearing any RequestData from health check.");
    mockedIngestion.resetData();
  }

  private void waitForHealthCheckTelemetryIfNeeded(String contextRootUrl)
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

  protected void setupProperties(String imageName, String imageAppDir) {
    appFile = new File(System.getProperty("ai.smoke-test.test-app-file"));
    javaagentFile = new File(System.getProperty("ai.smoke-test.javaagent-file"));
    currentImageName = imageName;
    currentImageAppDir = imageAppDir;
  }

  protected void startMockedIngestion() throws Exception {
    mockedIngestion.addIngestionFilter(
        new Predicate<Envelope>() {
          @Override
          public boolean test(Envelope input) {
            if (input == null) {
              return false;
            }
            String deviceId = input.getTags().get("ai.device.id");
            if (deviceId == null) {
              return true;
            }
            GenericContainer<?> container = targetContainer.get();
            if (container == null) { // ignore telemetry in after container is cleaned up.
              return false;
            }
            boolean belongsToCurrentContainer = container.getContainerId().startsWith(deviceId);
            if (!belongsToCurrentContainer) {
              System.out.println("Telemetry from previous container");
            }
            return belongsToCurrentContainer;
          }
        });
    mockedIngestion.startServer();
  }

  private void createDockerNetwork() {
    System.out.println("Creating network...");
    network = Network.newNetwork();
  }

  private void cleanUpDockerNetwork() {
    if (network == null) {
      return;
    }
    try {
      network.close();
    } finally {
      network = null;
    }
  }

  private void startAllContainers() throws Exception {
    startDependencyContainers();
    startTestApplicationContainer();
  }

  private void startDependencyContainers() {
    for (DependencyContainer dc : dependencyImages) {
      String imageName = dc.imageName().isEmpty() ? dc.value() : dc.imageName();
      System.out.println("Starting container: " + imageName);
      String containerName = "dependency" + new Random().nextInt(Integer.MAX_VALUE);
      String[] envVars = substitue(dc.environmentVariables(), hostnameEnvVars, containerName);
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
      allContainers.push(container);
    }
  }

  private static String[] substitue(
      String[] environmentVariables, Map<String, String> hostnameEnvVars, String containerName) {
    String[] envVars = new String[environmentVariables.length];
    for (int i = 0; i < environmentVariables.length; i++) {
      envVars[i] = substitute(environmentVariables[i], hostnameEnvVars, containerName);
    }
    return envVars;
  }

  private static String substitute(
      String environmentVariable, Map<String, String> hostnameEnvVars, String containerName) {
    String envVar = environmentVariable;
    for (Map.Entry<String, String> entry : hostnameEnvVars.entrySet()) {
      envVar = envVar.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    envVar = envVar.replace("${CONTAINERNAME}", containerName);
    return envVar;
  }

  @SuppressWarnings(
      "deprecation") // intentionally using FixedHostPortGenericContainer when remote debugging
  // enabled
  private void startTestApplicationContainer() throws Exception {
    System.out.println("Starting app container");

    Testcontainers.exposeHostPorts(6060);

    GenericContainer<?> container;
    if (REMOTE_DEBUG) {
      container =
          new FixedHostPortGenericContainer<>(currentImageName).withFixedExposedPort(5005, 5005);
    } else {
      container = new GenericContainer<>(currentImageName);
    }

    container =
        container
            .withEnv(hostnameEnvVars)
            .withEnv(
                "APPLICATIONINSIGHTS_CONNECTION_STRING",
                "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF;"
                    + "IngestionEndpoint=http://host.testcontainers.internal:6060/")
            .withEnv("APPLICATIONINSIGHTS_ROLE_NAME", "testrolename")
            .withEnv("APPLICATIONINSIGHTS_ROLE_INSTANCE", "testroleinstance")
            .withNetwork(network)
            .withExposedPorts(8080)
            .withCopyFileToContainer(
                MountableFile.forHostPath(appFile.toPath()),
                currentImageAppDir + "/" + appFile.getName());

    List<String> javaToolOptions = new ArrayList<>();
    javaToolOptions.add("-Dapplicationinsights.testing.batch-schedule-delay-millis=500");
    if (REMOTE_DEBUG) {
      javaToolOptions.add("-agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=y");
    }
    if (useAgent) {
      javaToolOptions.add("-javaagent:/applicationinsights-agent.jar");
    }
    container.withEnv("JAVA_TOOL_OPTIONS", String.join(" ", javaToolOptions));

    if (useAgent) {
      container =
          container.withCopyFileToContainer(
              MountableFile.forHostPath(javaagentFile.toPath()), "/applicationinsights-agent.jar");
      URL resource = AiSmokeTest.class.getClassLoader().getResource(agentConfigurationPath);
      if (resource != null) {
        File json = File.createTempFile("applicationinsights", ".json");
        Path jsonPath = json.toPath();
        try (InputStream in = resource.openStream()) {
          Files.copy(in, jsonPath, StandardCopyOption.REPLACE_EXISTING);
        }
        container =
            container.withCopyFileToContainer(
                MountableFile.forHostPath(jsonPath), "/applicationinsights.json");
      }
    }

    if (appFile.getName().endsWith(".jar")) {
      container = container.withCommand("java -jar " + appFile.getName());
    }

    Stopwatch stopwatch = Stopwatch.createStarted();
    container.start();
    System.out.printf(
        "App container (%s) started after %.3f seconds%n",
        container.getContainerId(), stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0);

    appServerPort = container.getMappedPort(8080);

    targetContainer.set(container);
    allContainers.push(container);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    mockedIngestion.resetData();
    System.out.println("Mocked ingestion reset.");
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    stopAllContainers();
    cleanUpDockerNetwork();
    mockedIngestion.stopServer();
    mockedIngestion.setRequestLoggingEnabled(false);
  }

  public void stopAllContainers() {
    if (allContainers.isEmpty()) {
      System.out.println("No containers to stop");
      return;
    }

    System.out.println("Stopping containers");
    while (!allContainers.isEmpty()) {
      GenericContainer<?> c = allContainers.pop();
      if (c.equals(targetContainer.get())) {
        targetContainer.set(null);
      }
      c.stop();
      if (c.isRunning()) {
        System.err.printf("ERROR: Container failed to stop: " + c.getContainerName());
      }
    }
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  protected static <T extends Domain> T getBaseData(Envelope envelope) {
    return ((Data<T>) envelope.getData()).getBaseData();
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
    AiSmokeTest.assertParentChild(
        rd.getId(), parentEnvelope, childEnvelope, operationName, operationName, true);
  }

  public static void assertParentChild(
      RemoteDependencyData rdd,
      Envelope parentEnvelope,
      Envelope childEnvelope,
      String operationName) {
    AiSmokeTest.assertParentChild(
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
    assertNotNull(operationId);
    assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));

    String operationParentId = parentEnvelope.getTags().get("ai.operation.parentId");
    if (topLevelParent) {
      assertNull(operationParentId);
    } else {
      assertNotNull(operationParentId);
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
