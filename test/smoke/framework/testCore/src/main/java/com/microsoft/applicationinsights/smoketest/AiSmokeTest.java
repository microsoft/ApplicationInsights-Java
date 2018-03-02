package com.microsoft.applicationinsights.smoketest;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.docker.AiDockerClient;
import com.microsoft.applicationinsights.smoketest.docker.ContainerInfo;
import com.microsoft.applicationinsights.smoketest.fixtures.AfterWithParams;
import com.microsoft.applicationinsights.smoketest.fixtures.BeforeWithParams;
import com.microsoft.applicationinsights.smoketest.fixtures.ParameterizedRunnerWithFixturesFactory;
import com.microsoft.applicationinsights.test.fakeingestion.MockedAppInsightsIngestionServer;
import com.microsoft.applicationinsights.test.fakeingestion.MockedAppInsightsIngestionServlet;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import javax.annotation.Nullable;
import javax.swing.plaf.synth.SynthConstants;
import javax.transaction.NotSupportedException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
/**
 * This is the base class for smoke tests.
 */
@RunWith(Parameterized.class)
@UseParametersRunnerFactory(ParameterizedRunnerWithFixturesFactory.class)
public abstract class AiSmokeTest {

	//region: parameterization
	@Parameters(name = "{index}: {0}, {1}, {2}")
	public static Collection<Object[]> parameterGenerator() throws MalformedURLException, IOException {
		List<String> appServers = Resources.readLines(Resources.getResource("appServers.txt"), Charsets.UTF_8);
		String os = System.getProperty("applicationinsights.smoketest.os", "linux");
		Multimap<String, String> appServers2jres = HashMultimap.create();
		for (String appServer : appServers) {
			List<String> serverJres = getAppServerJres(appServer);
			appServers2jres.putAll(appServer, serverJres);
		}

		Collection<Object[]> rval = new ArrayList<>();

		// keys = appServers, values = jres supported by appServer
		for (Entry<String, String> entry : appServers2jres.entries()) {
			rval.add(new Object[]{entry.getKey(), os, entry.getValue()});
		}

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

	// protected static Stack<ContainerInfo> containerStack = new Stack<>();
	protected static short currentPortNumber = BASE_PORT_NUMBER;
	
	protected static ContainerInfo currentContainerInfo = null;
	protected static String currentImageName;
	protected static short appServerPort;
	protected static String warFileName;
	//endregion

	//region: application fields


	protected String targetUri;
	protected String httpMethod;
	protected boolean expectSomeTelemetry = true;
	//endregion

	//region: options
	public static final int APPLICATION_READY_TIMEOUT_SECONDS = 120;
	public static final int TELEMETRY_RECEIVE_TIMEOUT_SECONDS = 10;
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
			TargetUri targetUri = description.getAnnotation(TargetUri.class);
			AiSmokeTest thiz = AiSmokeTest.this;
			if (targetUri == null) {
				thiz.targetUri = null;
				thiz.httpMethod = null;
			} else {
				thiz.targetUri = targetUri.value();
				if (!thiz.targetUri.startsWith("/")) {
					thiz.targetUri = "/"+thiz.targetUri;
				}

				thiz.httpMethod = targetUri.method().toUpperCase();
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

	@BeforeWithParams
	public static void configureEnvironment(final String appServer, final String os, final String jreVersion) throws Exception {
		System.out.println("Preparing environment...");
		if (currentContainerInfo != null) {
			// FIXME test cleanup didn't take...try to clean up
		}
		checkParams(appServer, os, jreVersion);
		setupProperties(appServer, os, jreVersion);
		startMockedIngestion();
		startDockerContainer();
		waitForApplicationToStart();
		System.out.println("Environment preparation complete.");
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
		waitForUrl(getBaseUrl(), APPLICATION_READY_TIMEOUT_SECONDS, TimeUnit.SECONDS, getAppContext());
		System.out.println("Test app health check complete.");
	}

	protected void callTargetUriAndWaitForTelemetry() throws Exception {
		if (targetUri == null) {
			System.out.println("targetUri==null: automated testapp request disabled");
			return;
		}

		System.out.println("Calling "+targetUri+" ...");
		String url = getBaseUrl()+targetUri;
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

		System.out.printf("Waiting %ds for telemetry...", TELEMETRY_RECEIVE_TIMEOUT_SECONDS);
		TimeUnit.SECONDS.sleep(TELEMETRY_RECEIVE_TIMEOUT_SECONDS);
		System.out.println("Finished waiting for telemetry.%nStarting validation...");

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

	protected static void startDockerContainer() throws Exception {
		System.out.printf("Starting container: %s%n", currentImageName);
		String containerId = docker.startContainer(currentImageName, appServerPort+":8080");
		assertFalse("'containerId' was null/empty attempting to start container: "+currentImageName, Strings.isNullOrEmpty(containerId));
		System.out.println("Container started: "+containerId);

		currentContainerInfo = new ContainerInfo(containerId, currentImageName);
		try {
			String url = String.format("http://localhost:%s/", String.valueOf(appServerPort));
			System.out.printf("Waiting for appserver to start (%s)...%n", url);

			waitForUrl(url, 90, TimeUnit.SECONDS, "app server");// TODO change to actual app server name
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
		// TODO start application dependencies---container(s)
	}
	//endregion

	protected void doCalcSendsData() throws Exception {
		System.out.println("Wait for app to finish deploying...");
		String appContext = warFileName.replace(".war", "");
		String baseUrl = "http://localhost:" + appServerPort + "/" + appContext;
		waitForUrl(baseUrl, 60, TimeUnit.SECONDS, appContext);
		System.out.println("Test app health check complete.");

		String url = baseUrl+"/doCalc?leftOperand=1&rightOperand=2&operator=plus";
		String content = HttpHelper.get(url);

		assertNotNull(content);
		assertTrue(content.length() > 0);
		
		System.out.println("Waiting 10s for telemetry...");
		TimeUnit.SECONDS.sleep(10);
		System.out.println("Finished waiting for telemetry. Starting validation...");

		assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
		assertTrue("mocked ingestion has 0 items", mockedIngestion.getItemCount() > 0);	
	}

	@After
	public void resetMockedIngestion() throws Exception {
		mockedIngestion.resetData();
		System.out.println("Mocked ingestion reset.");
	}

	@AfterWithParams
	public static void tearDownContainer(final String appServer, final String os, final String jreVersion) throws Exception {
		stopContainer(currentContainerInfo);
		if (!docker.isContainerRunning(currentContainerInfo.getContainerId())) { // for good measure
			currentContainerInfo = null;
		}
	}

	//region: test helper methods
	/// This section has methods to be used inside tests ///

	protected static String getProperty(String key) {
		String rval = testProps.getProperty(key);
		if (rval == null) throw new RuntimeException(String.format("test property not found '%s'", key));
		return rval;
	}

	protected static void waitForUrl(String url, long timeout, TimeUnit timeoutUnit, String appName) throws InterruptedException {
		String rval = null;
		Stopwatch watch = Stopwatch.createStarted();
		do {
			if (watch.elapsed(timeoutUnit) > timeout) {
				throw new RuntimeException(String.format("Timeout reached waiting for '%s' to come online", appName));
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
		assertFalse(String.format("Empty response from '%s'. HealthCheck urls should return something non-empty", url), rval.isEmpty());
	}

	protected <T extends Domain> T getTelemetryDataForType(int index, String type) {
		return mockedIngestion.getBaseDataForType(index, type);
	}
	//endregion
}