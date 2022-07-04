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
import com.microsoft.applicationinsights.smoketest.fixtures.AfterWithParams;
import com.microsoft.applicationinsights.smoketest.fixtures.BeforeWithParams;
import com.microsoft.applicationinsights.smoketest.fixtures.ParameterizedRunnerWithFixturesFactory;
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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/** This is the base class for smoke tests. */
@RunWith(Parameterized.class)
@UseParametersRunnerFactory(ParameterizedRunnerWithFixturesFactory.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({"SystemOut", "InterruptedExceptionSwallowed"})
public abstract class AiSmokeTest {

  // use -PsmokeTestMatrix=true at gradle command line to enable (see ai.smoke-test.gradle.kts)
  protected static final boolean USE_MATRIX = Boolean.getBoolean("ai.smoke-test.matrix");

  // use -PsmokeTestRemoteDebug=true at gradle command line to enable (see ai.smoke-test.gradle.kts)
  private static final boolean REMOTE_DEBUG = Boolean.getBoolean("ai.smoke-test.remote-debug");

  @Parameter(0)
  public String imageName;

  @Parameter(1)
  public String imageAppDir;

  private static final List<DependencyContainer> dependencyImages = new ArrayList<>();
  private static final AtomicReference<GenericContainer<?>> targetContainer =
      new AtomicReference<>();
  private static final Deque<GenericContainer<?>> allContainers = new ArrayDeque<>();
  private static final Map<String, String> hostnameEnvVars = new HashMap<>();
  protected static String currentImageName;
  private static String currentImageAppDir;
  private static int appServerPort;
  private static File appFile;
  private static File javaagentFile;

  private static boolean useAgent;
  @Nullable private static String agentConfigurationPath;
  @Nullable private static Network network;

  @Nullable private String targetUri;
  private long targetUriCallCount;

  private static final int TELEMETRY_RECEIVE_TIMEOUT_SECONDS = 60;

  protected static final MockedAppInsightsIngestionServer mockedIngestion =
      new MockedAppInsightsIngestionServer();

  /**
   * This rule does a few things: 1. failure detection: logs are only grabbed when the test fails.
   * 2. reads test metadata from annotations
   */
  @Rule
  public TestWatcher theWatchman =
      new TestWatcher() {

        @Override
        protected void starting(Description description) {
          System.out.println("Configuring test...");
          TargetUri targetUri = description.getAnnotation(TargetUri.class);
          AiSmokeTest thiz = AiSmokeTest.this;
          if (targetUri == null) {
            thiz.targetUri = null;
            thiz.targetUriCallCount = 1;
          } else {
            thiz.targetUri = targetUri.value();
            if (!thiz.targetUri.startsWith("/")) {
              thiz.targetUri = "/" + thiz.targetUri;
            }
            thiz.targetUriCallCount = targetUri.callCount();
          }
        }

        @Override
        protected void failed(Throwable t, Description description) {
          // NOTE this happens after @After :)
          System.out.println("Test failure detected.");
          System.out.println("Container logs:");
          System.out.println(targetContainer.get().getLogs());
        }
      };

  @BeforeClass
  public static void configureShutdownHook() {
    // NOTE the JUnit runner (or gradle) forces this to happen. The synchronized block and check for
    // empty should avoid any issues
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  GenericContainer<?> container = targetContainer.get();
                  if (container == null) {
                    return;
                  }
                  try {
                    container.stop();
                  } catch (RuntimeException e) {
                    System.err.println(
                        "Error while stopping container id="
                            + container.getContainerId()
                            + ". This must be stopped manually.");
                    e.printStackTrace();
                  }
                }));
  }

  @ClassRule
  public static TestWatcher classOptionsLoader =
      new TestWatcher() {
        @Override
        protected void starting(Description description) {
          UseAgent ua = description.getAnnotation(UseAgent.class);
          if (ua != null) {
            useAgent = true;
            agentConfigurationPath = ua.value();
          }
          WithDependencyContainers wdc = description.getAnnotation(WithDependencyContainers.class);
          if (wdc != null) {
            Collections.addAll(dependencyImages, wdc.value());
          }
        }

        @Override
        protected void finished(Description description) {
          dependencyImages.clear();
          useAgent = false;
          agentConfigurationPath = null;
        }
      };

  @BeforeWithParams
  public static void configureEnvironment(String imageName, String imageAppDir) throws Exception {
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

  @Before
  public void setupTest() throws Exception {
    callTargetUriAndWaitForTelemetry();
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

  protected static String getBaseUrl() {
    String appContext = getAppContext();
    if (appContext.isEmpty()) {
      return "http://localhost:" + appServerPort;
    } else {
      return "http://localhost:" + appServerPort + "/" + appContext;
    }
  }

  private static void clearOutAnyInitLogs() throws Exception {
    String contextRootUrl = getBaseUrl() + "/";
    HttpHelper.getResponseCodeEnsuringSampled(contextRootUrl);
    waitForHealthCheckTelemetryIfNeeded(contextRootUrl);
    System.out.println("Clearing any RequestData from health check.");
    mockedIngestion.resetData();
  }

  private static void waitForHealthCheckTelemetryIfNeeded(String contextRootUrl)
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

  protected void callTargetUriAndWaitForTelemetry() throws Exception {
    if (targetUri == null) {
      System.out.println("targetUri==null: automated testapp request disabled");
      return;
    }
    System.out.println("Calling " + targetUri + " ...");
    String url = getBaseUrl() + targetUri;
    if (targetUriCallCount == 1) {
      System.out.println("calling " + url);
    } else {
      System.out.println("calling " + url + " " + targetUriCallCount + " times");
    }
    for (int i = 0; i < targetUriCallCount; i++) {
      String content = HttpHelper.get(url);
      String expectationMessage = "The base context in testApps should return a nonempty response.";
      assertNotNull(
          String.format("Null response from targetUri: '%s'. %s", targetUri, expectationMessage),
          content);
      assertTrue(
          String.format("Empty response from targetUri: '%s'. %s", targetUri, expectationMessage),
          content.length() > 0);
    }
    Stopwatch sw = Stopwatch.createStarted();
    mockedIngestion.awaitAnyItems(TELEMETRY_RECEIVE_TIMEOUT_SECONDS * 1000, TimeUnit.MILLISECONDS);
    System.out.printf(
        "Telemetry received after %.3f seconds.%n", sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0);
    System.out.println("Starting validation...");
    assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
  }

  protected static void setupProperties(String imageName, String imageAppDir) {
    appFile = new File(System.getProperty("ai.smoke-test.test-app-file"));
    javaagentFile = new File(System.getProperty("ai.smoke-test.javaagent-file"));
    currentImageName = imageName;
    currentImageAppDir = imageAppDir;
  }

  protected static void startMockedIngestion() throws Exception {
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

  private static void createDockerNetwork() {
    System.out.println("Creating network...");
    network = Network.newNetwork();
  }

  private static void cleanUpDockerNetwork() {
    if (network == null) {
      return;
    }
    try {
      network.close();
    } finally {
      network = null;
    }
  }

  private static void startAllContainers() throws Exception {
    startDependencyContainers();
    startTestApplicationContainer();
  }

  private static void startDependencyContainers() {
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
          "Dependency container %s started after %.3f seconds%n",
          imageName, stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0);

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

  @SuppressWarnings("deprecation") // intentionally using FixedHostPortGenericContainer
  private static void startTestApplicationContainer() throws Exception {
    System.out.println("Starting container: " + currentImageName);

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
        "App container started after %.3f seconds%n",
        stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0);

    appServerPort = container.getMappedPort(8080);

    System.out.println("Container started: " + currentImageName);

    targetContainer.set(container);
    allContainers.push(container);
  }

  @After
  public void resetMockedIngestion() {
    mockedIngestion.resetData();
    System.out.println("Mocked ingestion reset.");
  }

  @AfterWithParams
  public static void tearDownContainer(
      @SuppressWarnings("unused") String imageName, @SuppressWarnings("unused") String imageAppDir)
      throws Exception {
    stopAllContainers();
    cleanUpDockerNetwork();
    mockedIngestion.stopServer();
    mockedIngestion.setRequestLoggingEnabled(false);
  }

  public static void stopAllContainers() {
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

  @SuppressWarnings("TypeParameterUnusedInFormals")
  protected <T extends Domain> T getTelemetryDataForType(int index, String type) {
    return mockedIngestion.getBaseDataForType(index, type);
  }

  protected static Telemetry getTelemetry(int rddCount) throws Exception {
    return getTelemetry(rddCount, rdd -> true);
  }

  protected static Telemetry getTelemetry(int rddCount, Predicate<RemoteDependencyData> condition)
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

  public static class Telemetry {
    public Envelope rdEnvelope;
    public Envelope rddEnvelope1;
    public Envelope rddEnvelope2;
    public Envelope rddEnvelope3;

    public RequestData rd;
    public RemoteDependencyData rdd1;
    public RemoteDependencyData rdd2;
    public RemoteDependencyData rdd3;
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
