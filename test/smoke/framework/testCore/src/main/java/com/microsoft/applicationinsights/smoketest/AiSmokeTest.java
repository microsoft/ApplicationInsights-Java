package com.microsoft.applicationinsights.smoketest;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.docker.AiDockerClient;
import com.microsoft.applicationinsights.smoketest.docker.ContainerInfo;
import com.microsoft.applicationinsights.smoketest.exceptions.SmokeTestException;
import com.microsoft.applicationinsights.smoketest.exceptions.TimeoutException;
import com.microsoft.applicationinsights.smoketest.fixtures.AfterWithParams;
import com.microsoft.applicationinsights.smoketest.fixtures.BeforeWithParams;
import com.microsoft.applicationinsights.smoketest.fixtures.ParameterizedRunnerWithFixturesFactory;
import com.microsoft.applicationinsights.test.fakeingestion.MockedAppInsightsIngestionServer;
import com.microsoft.applicationinsights.test.fakeingestion.MockedAppInsightsIngestionServlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import javax.annotation.Nullable;
import javax.transaction.NotSupportedException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;
/**
 * This is the base class for smoke tests.
 */
@RunWith(Parameterized.class)
@UseParametersRunnerFactory(ParameterizedRunnerWithFixturesFactory.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AiSmokeTest {

	//region: parameterization
	@Parameters(name = "{index}: {0}, {1}, {2}")
	public static Collection<Object[]> parameterGenerator() throws IOException {
		List<String> appServers = Resources.readLines(Resources.getResource("appServers.txt"), Charsets.UTF_8);
		System.out.println("Target appservers="+Arrays.toString(appServers.toArray()));
		String os = System.getProperty("applicationinsights.smoketest.os", "linux");
		Multimap<String, String> appServers2jres = HashMultimap.create();
		for (String appServer : appServers) {
			List<String> serverJres;
			try {
				serverJres = getAppServerJres(appServer);
			} catch (Exception e) {
				System.err.printf("SKIPPING '%s'. Could not configure jres: %s%n", appServer, e);
				continue;
			}
			appServers2jres.putAll(appServer, serverJres);
		}

		Collection<Object[]> rval = new ArrayList<>();

		// keys = appServers, values = jres supported by appServer
		for (Entry<String, String> entry : appServers2jres.entries()) {
			rval.add(new Object[]{entry.getKey(), os, entry.getValue()});
		}
		System.out.println("Configured appservers="+Arrays.toString(appServers2jres.keySet().toArray()));

		return rval;
	}

	private static List<String> getAppServerJres(String appServer) throws IOException {
		List<String> rval = Resources.readLines(Resources.getResource(appServer+".jre.txt"), Charsets.UTF_8);
		return Lists.transform(rval, new Function<String, String>() {
			@Override
			public String apply(String input) {
				return input.replaceAll("[:/]", "_");
			}
		});
	}
	
	@Parameter(0) public String appServer;
	@Parameter(1) public String os;
	@Parameter(2) public String jreVersion;
	//endregion

	//region: container fields
	private static final short BASE_PORT_NUMBER = 28080;
	private static final String TEST_CONFIG_FILENAME = "testInfo.properties";

	// TODO make this dependent on container mode
	private static final AiDockerClient docker = AiDockerClient.createLinuxClient();

	protected static void stopContainer(ContainerInfo info) throws Exception {
		System.out.printf("Stopping container: %s%n", info);
		Stopwatch killTimer = Stopwatch.createUnstarted();
		try {
			killTimer.start();
			docker.stopContainer(info.getContainerId());
			System.out.printf("Container stopped (%s) in %dms%n", info, killTimer.elapsed(TimeUnit.MILLISECONDS));
		}
		catch (Exception e) {
			System.err.printf("Error stopping container (in %dms): %s%n", killTimer.elapsed(TimeUnit.MILLISECONDS), info);
			throw e;
		}
	}

	protected static short currentPortNumber = BASE_PORT_NUMBER;

	private static List<DependencyContainer> dependencyImages = new ArrayList<>();
	protected static ContainerInfo currentContainerInfo = null;
	protected static Deque<ContainerInfo> allContainers = new ArrayDeque<>();
	protected static String currentImageName;
	protected static short appServerPort;
	protected static String warFileName;
	protected static String agentMode;
	protected static String networkId;
	protected static String networkName = "aismoke-net";
	//endregion

	//region: application fields
	protected String targetUri;
	protected String httpMethod;
	protected long targetUriDelayMs;
	protected boolean expectSomeTelemetry = true;
	//endregion

	//region: options
	public static final int APPLICATION_READY_TIMEOUT_SECONDS = 120;
	public static final int TELEMETRY_RECEIVE_TIMEOUT_SECONDS = 10;
	public static final int DELAY_AFTER_CONTAINER_STOP_MILLISECONDS = 1500;
	public static final int HEALTH_CHECK_RETRIES = 2;
	//endregion

	private static final Properties testProps = new Properties();

	protected static final MockedAppInsightsIngestionServer mockedIngestion = new MockedAppInsightsIngestionServer();

	/**
	 * This rule does a few things:
	 * 1. failure detection: logs are only grabbed when the test fails.
	 * 2. reads test metadata from annotations
	 */
	@Rule
	public TestWatcher theWatchman = new TestWatcher() {
		@Override
		protected void starting(Description description) {
			System.out.println("Configuring test...");
			TargetUri targetUri = description.getAnnotation(TargetUri.class);
			AiSmokeTest thiz = AiSmokeTest.this;
			if (targetUri == null) {
				thiz.targetUri = null;
				thiz.httpMethod = null;
				thiz.targetUriDelayMs = 0L;
			} else {
				thiz.targetUri = targetUri.value();
				if (!thiz.targetUri.startsWith("/")) {
					thiz.targetUri = "/"+thiz.targetUri;
				}
				thiz.httpMethod = targetUri.method().toUpperCase();
				thiz.targetUriDelayMs = targetUri.delay();
			}

			ExpectSomeTelemetry expectSomeTelemetry = description.getAnnotation(ExpectSomeTelemetry.class);
			thiz.expectSomeTelemetry = expectSomeTelemetry == null || expectSomeTelemetry.value();
		}

		@Override
		protected void failed(Throwable t, Description description) {
			// NOTE this happens after @After :)
			String containerId = currentContainerInfo.getContainerId();
			System.out.println("Test failure detected.");
			
			System.out.println("\nFetching appserver logs");
			try {
				docker.execOnContainer(containerId, docker.getShellExecutor(), "tailLastLog.sh");
			}
			catch (Exception e) {
				System.err.println("Error executing tailLastLog.sh");
				e.printStackTrace();
			}
			
			try {
				System.out.println("\nFetching container logs for "+containerId);
				docker.printContainerLogs(containerId);

			}
			catch (Exception e) {
				System.err.println("Error copying logs to stream");
				e.printStackTrace();
			}
			finally {
				System.out.println("\nFinished gathering logs.");
			}
		}
	};

	@BeforeClass
	public static void configureShutdownHook() {
		// NOTE the JUnit runner (or gradle) forces this to happen. The syncronized block and check for empty should avoid any issues
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				if (currentContainerInfo == null) {
					return;
				}
				try {
					stopContainer(currentContainerInfo);
				} catch (Exception e) {
					System.err.println("Error while stopping container id="+currentContainerInfo.getContainerId()+". This must be stopped manually.");
					e.printStackTrace();
				}
			}
		}));
	}

	@ClassRule
	public static TestWatcher classOptionsLoader = new TestWatcher() {
		@Override
		protected void starting(Description description) {
			System.out.println("Configuring test class...");
			UseAgent ua = description.getAnnotation(UseAgent.class);
			if (ua != null) {
				agentMode = ua.value();
				System.out.println("AGENT MODE: "+agentMode);
			}
			WithDependencyContainers wdc = description.getAnnotation(WithDependencyContainers.class);
			if (wdc != null) {
				for (DependencyContainer container : wdc.value()) {
					if (StringUtils.isBlank(container.value())) { // checks for null
						System.err.printf("WARNING: skipping dependency container with invalid name: '%s'%n", container.value());
						continue;
					}
					dependencyImages.add(container);
				}
			}
		}

		@Override
		protected void finished(Description description) {
			String message = "";
			if (agentMode != null) {
				message += "Resetting agentMode. ";
			}
			if (!dependencyImages.isEmpty()) {
				message += "Clearing dependency images. ";
			}
			System.out.printf("Finished test class. %s%n", message);
			dependencyImages.clear();
			agentMode = null;
		}
	};

	@BeforeWithParams
	public static void configureEnvironment(final String appServer, final String os, final String jreVersion) throws Exception {
		System.out.println("Preparing environment...");
		try {
			if (currentContainerInfo != null) {
				// test cleanup didn't take...try to clean up
				if (docker.isContainerRunning(currentContainerInfo.getContainerId())) {
					System.err.println("From last test run, container is still running: " + currentContainerInfo);
					try {
						docker.stopContainer(currentContainerInfo.getContainerId());
					} catch (Exception e) {
						System.err.println("Couldn't clean up environment. Must be done manually.");
						throw e;
					}
				} else {
					// container must have stopped after timeout reached.
					currentContainerInfo = null;
				}
			}
			checkParams(appServer, os, jreVersion);
			setupProperties(appServer, os, jreVersion);
			startMockedIngestion();
			createDockerNetwork();
			startAllContainers();
			waitForApplicationToStart();
			System.out.println("Environment preparation complete.");
		} catch (Exception e) {
			System.err.printf("Could not configure environment: %s%n", ExceptionUtils.getStackTrace(e));
			throw e;
		}
	}

	
	@Before
	public void setupTest() throws Exception {
		callTargetUriAndWaitForTelemetry();
	}

	//region: before test helper methods
	protected static String getAppContext() {
		return warFileName.replace(".war", "");
	}

	protected static String getBaseUrl() {
		return "http://localhost:" + appServerPort + "/" + getAppContext();
	}

	protected static void waitForApplicationToStart() throws Exception {
		System.out.printf("Test app health check: Waiting for %s to start...%n", warFileName);
		waitForUrlWithRetries(getBaseUrl(), APPLICATION_READY_TIMEOUT_SECONDS, TimeUnit.SECONDS, getAppContext(), HEALTH_CHECK_RETRIES);
		System.out.println("Test app health check complete.");
		System.out.printf("Waiting %ds for any request telemetry...", TELEMETRY_RECEIVE_TIMEOUT_SECONDS);
		TimeUnit.SECONDS.sleep(TELEMETRY_RECEIVE_TIMEOUT_SECONDS);
		System.out.println("Clearing any RequestData from health check.");
		mockedIngestion.resetData();
	}

	protected void callTargetUriAndWaitForTelemetry() throws Exception {
		if (targetUri == null) {
			System.out.println("targetUri==null: automated testapp request disabled");
			return;
		}
		if (targetUriDelayMs > 0) {
			System.out.printf("Waiting %.3fs before calling uri...%n", targetUriDelayMs/1000.0);
			System.out.flush();
			TimeUnit.MILLISECONDS.sleep(targetUriDelayMs);
		}
		System.out.println("Calling "+targetUri+" ...");
		String url = getBaseUrl()+targetUri;
    	System.out.println("calling " + url);
		final String content;
		switch(httpMethod) {
			case "GET":
				content = HttpHelper.get(url);
				break;
			default:
				throw new NotSupportedException(String.format("http method '%s' is not currently supported", httpMethod));
		}

		String expectationMessage = "The base context in testApps should return a nonempty response.";
		assertNotNull(String.format("Null response from targetUri: '%s'. %s", targetUri, expectationMessage), content);
		assertTrue(String.format("Empty response from targetUri: '%s'. %s", targetUri, expectationMessage), content.length() > 0);

		System.out.printf("Waiting %ds for telemetry...%n", TELEMETRY_RECEIVE_TIMEOUT_SECONDS);
		TimeUnit.SECONDS.sleep(TELEMETRY_RECEIVE_TIMEOUT_SECONDS);
		System.out.println("Finished waiting for telemetry.\nStarting validation...");

		if (expectSomeTelemetry) {
			assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
		}
	}

	protected static void checkParams(final String appServer, final String os, final String jreVersion) {
		String fmt = "Missing required framework parameter: %s - this indicates an error in the parameter generator";
		assertNotNull(String.format(fmt, "appServer"), appServer);
		assertNotNull(String.format(fmt, "os"), os);
		assertNotNull(String.format(fmt, "jreVersion"), jreVersion);
	}

	protected void checkParams() {
		checkParams(this.appServer, this.os, this.jreVersion);
	}

	protected static void setupProperties(final String appServer, final String os, final String jreVersion) throws Exception {
		testProps.load(new FileReader(new File(Resources.getResource(TEST_CONFIG_FILENAME).toURI())));
		currentImageName = String.format("%s_%s_%s", appServer, os, jreVersion);
		appServerPort = currentPortNumber++;
	}

	protected static void startMockedIngestion() throws Exception {
		mockedIngestion.addIngestionFilter(new Predicate<Envelope>() {
			@Override
			public boolean apply(@Nullable Envelope input) {
				String deviceId = input.getTags().get("ai.device.id");
				if (deviceId == null) {
					return true;
				}
				final boolean belongsToCurrentContainer = currentContainerInfo.getContainerId().startsWith(deviceId);
				if (!belongsToCurrentContainer) {
					System.out.println("Telemetry from previous container");
				}
				return belongsToCurrentContainer;
			}
		});
		mockedIngestion.startServer();
		TimeUnit.SECONDS.sleep(2);
		checkMockedIngestionHealth();
	}

	protected static void checkMockedIngestionHealth() throws Exception {
		String ok = HttpHelper.get("http://localhost:"+mockedIngestion.getPort()+"/");
		assertEquals(MockedAppInsightsIngestionServlet.ENDPOINT_HEALTH_CHECK_RESPONSE, ok);
		String postResponse = HttpHelper.post("http://localhost:60606/v2/track", MockedAppInsightsIngestionServlet.PING);
		assertEquals(MockedAppInsightsIngestionServlet.PONG, postResponse);
	}

	private static void createDockerNetwork() throws Exception {
		try {
			System.out.printf("Creating network '%s'...%n", networkName);
			networkId = docker.createNetwork(networkName);
		} catch (Exception e) {
			System.err.printf("Error creating network named '%s'%n", networkName);
			e.printStackTrace();
			throw e;
		}
	}

	private static void cleanUpDockerNetwork() throws Exception {
		if (networkId == null) {
			System.out.println("No network id....nothing to clean up");
			return;
		}
		try {
			System.out.printf("Deleting network '%s'...%n", networkName);
			docker.deleteNetwork(networkName);
		} catch (Exception e) {
			System.err.printf("Error deleting network named '%s' (%s)%n", networkName, networkId);
			e.printStackTrace();
		} finally {
			networkId = null;
		}
	}

	private static void startAllContainers() throws Exception {
		startDependencyContainers();
		startTestApplicationContainer();
	}

	private static void startDependencyContainers() throws IOException, InterruptedException {
		if (dependencyImages.isEmpty()) {
			System.out.println("No dependency containers to start.");
			return;
		}

		for (DependencyContainer dc : dependencyImages) {
			String imageName = Strings.isNullOrEmpty(dc.imageName()) ? dc.value() : dc.imageName();
			System.out.printf("Starting container: %s%n", imageName);
			final String containerId = docker.startContainer(imageName, dc.portMapping(), networkId);
			assertFalse("'containerId' was null/empty attempting to start container: "+imageName, Strings.isNullOrEmpty(containerId));
			System.out.printf("Dependency container started: %s (%s)%n", imageName, containerId);

			String containerName = docker.getRunningContainerName(containerId);
			if (containerName == null) {
				String message = String.format("Could not get container name for id=%s. ", containerId);
				if (docker.isContainerRunning(containerId)) {
					message += "It appears to be running.";
				} else {
					message += "It appears to have stopped.";
				}
				System.err.println(message);
				throw new SmokeTestException("Couldn't get container name for image="+imageName);
			}
			ContainerInfo depConInfo = new ContainerInfo(containerId, containerName);
			depConInfo.setContainerName(containerName);
			depConInfo.setDependencyContainerInfo(dc);
			allContainers.push(depConInfo);
			TimeUnit.MILLISECONDS.sleep(500); // wait a bit after starting a server.
		}
	}

	private static void startTestApplicationContainer() throws Exception {
		System.out.printf("Starting container: %s%n", currentImageName);
		Map<String, String> envVars = generateAppContainerEnvVarMap();
		String containerId = docker.startContainer(currentImageName, appServerPort+":8080", networkId, null, envVars);
		assertFalse("'containerId' was null/empty attempting to start container: "+currentImageName, Strings.isNullOrEmpty(containerId));
		System.out.printf("Container started: %s (%s)%n", currentImageName, containerId);

		final int appServerDelayAfterStart_seconds = 5;
		System.out.printf("Waiting %d seconds for app server to startup...%n", appServerDelayAfterStart_seconds);
		TimeUnit.SECONDS.sleep(appServerDelayAfterStart_seconds);

		currentContainerInfo = new ContainerInfo(containerId, currentImageName);
		try {
			String url = String.format("http://localhost:%s/", String.valueOf(appServerPort));
			System.out.printf("Verifying appserver has started (%s)...%n", url);

			waitForUrlWithRetries(url, 120, TimeUnit.SECONDS, String.format("app server on image '%s'", currentImageName), HEALTH_CHECK_RETRIES);
			System.out.println("App server is ready.");
		}
		catch (Exception e) {
			System.err.println("Error starting app server");
			throw e;
		}

		try {
			warFileName = getProperty("ai.smoketest.testAppWarFile");
			System.out.printf("Deploying test application: %s...%n", warFileName);
			docker.copyAndDeployToContainer(containerId, new File(Resources.getResource(warFileName).toURI()));
			System.out.println("Test application deployed.");
		}
		catch (Exception e) {
			System.err.println("Error deploying test application.");
			throw e;
		}
		allContainers.push(currentContainerInfo);
	}

	private static Map<String, String> generateAppContainerEnvVarMap() {
		Map<String, String> map = new HashMap<>();
		if (agentMode != null) {
			map.put("AI_AGENT_MODE", agentMode);
		}
		for (ContainerInfo info : allContainers) {
			if (!info.isDependency()) {
				continue;
			}
			DependencyContainer dc = info.getDependencyContainerInfo();
			String varname = dc.environmentVariable();
			if (Strings.isNullOrEmpty(varname)) {
				varname = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, dc.value());
			}
			String containerName = info.getContainerName();
			if (Strings.isNullOrEmpty(containerName)) {
				throw new SmokeTestException("Null/empty container name for dependency container");
			}
			map.put(varname, info.getContainerName());
		}
		return map;
	}
	//endregion

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void zzz_CheckForExceptionsInLogs() throws IOException, InterruptedException {
	    Assume.assumeTrue("Test class opted to skip log scanner test.", shouldRunTheLogScannerTest());

		Stopwatch sw = Stopwatch.createStarted();
		System.out.println("\n-=== SCANNING LOGS FOR UNEXPECTED EXCEPTIONS ===-\n");
		System.out.println("Gathering logs...");
		docker.execOnContainer(currentContainerInfo.getContainerId(), "./gatherLogs.sh");
		System.out.println("Downloading zip...");
		File logsZipFile = tempFolder.newFile();
		docker.copyFromContainer(currentContainerInfo.getContainerId(), "/root/docker-stage/appServerLogs.zip", logsZipFile);
		System.out.println("Zipfile downloaded: "+logsZipFile.getAbsolutePath());
		ZipFile logsAsZip = new ZipFile(logsZipFile);

		int fileCount = 0;
		final AtomicInteger linesCount = new AtomicInteger();
		final AtomicInteger suppressedCount = new AtomicInteger();

		final Enumeration<? extends ZipEntry> entries = logsAsZip.entries();
		List<String> detectedErrors = new ArrayList<>();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			List<String> fileResults = CharStreams.readLines(new BufferedReader(new InputStreamReader(logsAsZip.getInputStream(entry))), new LineProcessor<List<String>>() {
				final List<String> result = new ArrayList<>();

				final List<Pattern> errorPatterns = new LinkedList<>();
				final List<Pattern> stackTracePatterns = new LinkedList<>();
				final List<Pattern> suppressionPatterns = new LinkedList<>();

				boolean lastOneWasSuppressed = false;

				{ // <init>
					errorPatterns.add(Pattern.compile(".*?ERROR\\s+.*Exception.*"));

					stackTracePatterns.add(Pattern.compile(".*\\s+at\\s+?(?:[A-Za-z][\\w$]+)(?:\\.(?:[A-Za-z][\\w$]+))*.*"));

					suppressionPatterns.add(Pattern.compile("failed to resolve instrumentation key"));
				}

				@Override
				public boolean processLine(final String line) {
					linesCount.incrementAndGet();
					// check if line matches an exception pattern
					if (anyPatternMatches(line, errorPatterns)) {
						if (anyPatternMatches(line, suppressionPatterns)) {
							suppressedCount.incrementAndGet();
						    lastOneWasSuppressed = true;
							return true;
						}
						result.add(line);
						lastOneWasSuppressed = false;
						return true;
					}

					if (lastOneWasSuppressed) { // then don't add the stack trace
						return true;
					}
					if (anyPatternMatches(line, stackTracePatterns)) {
						String lastResult = result.remove(result.size()-1);
						result.add(String.format("%s%n%s", lastResult, line));
					}
					return true;
				}

				private boolean anyPatternMatches(final String line, final List<Pattern> patterns) {
					for (Pattern p : patterns) {
						Matcher m = p.matcher(line);
						if (m.matches()) {
							return true;
						}
					}
					return false;
				}

				@Override
				public List<String> getResult() {
					return result;
				}
			});
			if (!fileResults.isEmpty()) {
				detectedErrors.addAll(fileResults);
			}
			fileCount++;
		}
		System.out.printf("Scanned %d lines in %d files (%d suppressed). This took %.3fms%n", linesCount.get(), fileCount, suppressedCount.get(), sw.elapsed(TimeUnit.NANOSECONDS)/1_000_000.0);
		if (!detectedErrors.isEmpty()) {
		    System.err.println("Errors detected:");
            for (int i = 0; i < detectedErrors.size(); i++) {
                String trace = detectedErrors.get(i);
                System.err.printf("%n%d:%n%s%n", i, trace);
            }
        }
		Assert.assertTrue(String.format("%d errors detected", detectedErrors.size()), detectedErrors.isEmpty());
		System.out.println("No errors detected.");
	}

    /**
     * Override this method if the smoketest class should not run the log scanner test.
     */
	protected boolean shouldRunTheLogScannerTest() {
	    return true;
    }

	@After
	public void resetMockedIngestion() throws Exception {
		mockedIngestion.resetData();
		System.out.println("Mocked ingestion reset.");
	}

	@AfterWithParams
	public static void tearDownContainer(final String appServer, final String os, final String jreVersion) throws Exception {
		stopAllContainers();
		cleanUpDockerNetwork();
		TimeUnit.MILLISECONDS.sleep(DELAY_AFTER_CONTAINER_STOP_MILLISECONDS);
	}

	public static void stopAllContainers() throws Exception {
		if (allContainers.isEmpty()) {
			System.out.println("No containers to stop");
			return;
		}

		// TODO depdency containers can be stopped in parallel
		System.out.printf("Stopping %d containers...", allContainers.size());
		List<ContainerInfo> failedToStop = new ArrayList<>();
		while (!allContainers.isEmpty()) {
			ContainerInfo c = allContainers.pop();
			if (currentContainerInfo == c) {
				System.out.println("Cleaning up app container");
				currentContainerInfo = null;
			}
			stopContainer(c);
			if (docker.isContainerRunning(c.getContainerId())) {
				System.err.printf("ERROR: Container failed to stop: %s%n", c.toString());
				failedToStop.add(c);
			}
		}

		if (currentContainerInfo != null) {
			System.err.println("Could not find app container in stack. Stopping...");
			stopContainer(currentContainerInfo);
			if (!docker.isContainerRunning(currentContainerInfo.getContainerId())) {
				currentContainerInfo = null;
			}
		}

		if (!failedToStop.isEmpty()) {
			System.err.println("Some containers failed to stop. Subsequent tests may fail.");
			for (ContainerInfo c : failedToStop) {
				if (docker.isContainerRunning(c.getContainerId())) {
					System.err.println("Failed to stop: "+c.toString());
				}
			}
		}
	}

	//region: test helper methods
	/// This section has methods to be used inside tests ///

	protected static String getProperty(String key) {
		String rval = testProps.getProperty(key);
		if (rval == null) throw new SmokeTestException(String.format("test property not found '%s'", key));
		return rval;
	}

	protected static <T extends Domain> T getBaseData(Envelope envelope) {
		return ((Data<T>)envelope.getData()).getBaseData();
	}

	protected static void waitForUrl(String url, long timeout, TimeUnit timeoutUnit, String appName) throws InterruptedException {
		String rval = null;
		Stopwatch watch = Stopwatch.createStarted();
		do {
			if (watch.elapsed(timeoutUnit) > timeout) {
				throw new TimeoutException(appName, timeout, timeoutUnit);
			}

			try {
				TimeUnit.SECONDS.sleep(1);
				rval = HttpHelper.get(url);
			}
			catch (InterruptedException ie) {
				throw ie;
			}
			catch (Exception e) {
				continue;
			}
			if (rval != null && rval.contains("404")) {
				rval = null;
			}
		} while (rval == null);
		assertFalse(String.format("Empty response from '%s'. Health check urls should return something non-empty", url), rval.isEmpty());
	}

	protected static void waitForUrlWithRetries(String url, long timeout, TimeUnit timeoutUnit, String appName, int numberOfRetries) {
		Preconditions.checkArgument(numberOfRetries >= 0, "numberOfRetries must be non-negative");
		int triedCount = 0;
		boolean success = false;
		Throwable lastThrowable = null;
		do {
			try {
				waitForUrl(url, timeout, timeoutUnit, appName);
				success = true;
			} catch (ThreadDeath td) {
				throw td;
			} catch (Throwable t) {
				lastThrowable = t;
				System.out.printf("WARNING: '%s' health check failed (%s). %d retries left. Exception: %s%n", appName, url, numberOfRetries-triedCount, t);
			}
		} while (!success && triedCount++ < numberOfRetries);
		if (!success) {
			throw new TimeoutException(appName, timeout*triedCount, timeoutUnit, String.format("Tried %d times to hit %s", triedCount, url), lastThrowable);
		}
	}

	protected <T extends Domain> T getTelemetryDataForType(int index, String type) {
		return mockedIngestion.getBaseDataForType(index, type);
	}
	//endregion
}