package com.microsoft.applicationinsights.smoketest;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.docker.AiDockerClient;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;
/**
 * This is the base class for smoke tests.
 */
@RunWith(Parameterized.class)
public abstract class AiSmokeTest {

	@Parameters(name = "{index}: {0}, {1}, {2}")
	public static Collection<Object[]> data() throws MalformedURLException, IOException {
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
				// FIXME some may have '/'. update to use regex
				return input.replace(':', '_');
			}
		});
	}
	
	@Parameter(0) public String appServer;
	
	@Parameter(1) public String os;
	
	@Parameter(2) public String jreVersion;

	private final static short BASE_PORT_NUMBER = 28080;
	private static final String TEST_CONFIG_FILENAME = "testInfo.properties";
	
	protected static short currentPortNumber = BASE_PORT_NUMBER;
	

	@Rule
	public TestWatcher theWatchman = new TestWatcher() {
		@Override
		protected void failed(Throwable t, Description description) {
			// NOTE this happens after @After :)
			String containerId = lastContainerId();
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
	
	protected static Stack<ContainerInfo> containerStack = new Stack<>();
	protected static String lastContainerId() {
		return containerStack.peek().getContainerId();
	}
	
	protected String warFileName;
	
	protected short appServerPort;
	protected String currentImageName;

	private final Properties testProps = new Properties();
	
	protected final MockedAppInsightsIngestion mockedIngestion = new MockedAppInsightsIngestion();

	protected class ContainerInfo {
		private final String containerId;
		private final String imageName;
		public ContainerInfo(String containerId, String imageName) {
			this.containerId = containerId;
			this.imageName = imageName;
		}
		/**
		 * @return the containerId
		 */
		public String getContainerId() {
			return containerId;
		}
		/**
		 * @return the imageName
		 */
		public String getImageName() {
			return imageName;
		}
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(ContainerInfo.class)
				.add("id", getContainerId())
				.add("image", getImageName())
				.toString();
		}
	}

	@BeforeClass
	public static void configureShutdownHook() {
		// NOTE the JUnit runner (or gradle) forces this to happen. The syncronized block and check for empty should avoid any issues
		Runtime.getRuntime().addShutdownHook(new Thread(destroyAllContainers));
	}

	@AfterClass
	public static void tearDownContainers() {
		destroyAllContainers.run();
	}

	protected static final Runnable destroyAllContainers = new Runnable() {
		@Override
		public void run() {
			synchronized (containerStack) {
				if (containerStack.isEmpty()) return;
				final int numToStop = containerStack.size();
				System.out.printf("Destroying all containers... (%d)%n", numToStop);
				ExecutorService taskService = Executors.newFixedThreadPool(containerStack.size());
				Stopwatch stopAllTimer = Stopwatch.createStarted();
				while (!containerStack.isEmpty()) {
					final ContainerInfo info = containerStack.pop();
					taskService.execute(new Runnable(){
						@Override
						public void run() {
							try {
								stopContainer(info);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
				taskService.shutdown();
				try {
					taskService.awaitTermination(numToStop * 15, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					// don't care
					System.err.println("Interrupted while stopping containers. There may still be containers running.");
					e.printStackTrace();
				}
				finally {
					stopAllTimer.stop();
				}
				System.out.printf("Stopping %d containers in parallel took %dms", numToStop, stopAllTimer.elapsed(TimeUnit.MILLISECONDS));
			}
		}
	};


	@Before
	public void setup() throws Exception {
		System.out.println("Preparing test...");
		checkParams();
		setupProperties();
		startMockedIngestion();
		startDocker();
		System.out.println("Test preperation complete");
		doCalcSendsData();
	}

	protected void checkParams() {
		assertNotNull("appServer", this.appServer);
		assertNotNull("os", this.os);
		assertNotNull("jreVersion", this.jreVersion);
	}

	protected void setupProperties() throws Exception {
		testProps.load(new FileReader(new File(Resources.getResource(TEST_CONFIG_FILENAME).toURI())));
		currentImageName = String.format("%s_%s_%s", this.appServer, this.os, this.jreVersion);
		appServerPort = currentPortNumber++;
	}

	protected void startMockedIngestion() throws Exception {
		mockedIngestion.startServer();
		TimeUnit.SECONDS.sleep(2);
		checkMockedIngestionHealth();
	}

	protected void checkMockedIngestionHealth() throws Exception {
		// TODO make the port configurable
		String ok = HttpHelper.get("http://localhost:60606/");
		assertEquals(MockedAppInsightsIngestion.ENDPOINT_HEALTH_CHECK_RESPONSE, ok);
		String postResponse = HttpHelper.post("http://localhost:60606/v2/track", MockedAppInsightsIngestion.PING);
		assertEquals(MockedAppInsightsIngestion.PONG, postResponse);
	}

	protected void startDocker() throws Exception {		
		System.out.printf("Starting container: %s%n", currentImageName);
		String containerId = docker.startContainer(currentImageName, appServerPort+":8080");
		assertFalse("'containerId' was null/empty attempting to start container: "+currentImageName, Strings.isNullOrEmpty(containerId));
		System.out.println("Container started: "+containerId);

		ContainerInfo info = new ContainerInfo(containerId, currentImageName);
		containerStack.push(info);
		try {
			String url = String.format("http://localhost:%s/", String.valueOf(this.appServerPort));
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
	public void tearDown() throws Exception {
		System.out.println("Cleaning up test resources...");
		resetMockedIngestion();
		System.out.println("Test resources cleaned.");
	}

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

	protected void resetMockedIngestion() throws Exception {
		mockedIngestion.stopServer();
		mockedIngestion.resetData();
	}

	protected String getProperty(String key) {
		String rval = testProps.getProperty(key);
		if (rval == null) throw new RuntimeException(String.format("test property not found '%s'", key));
		return rval;
	}

	protected void waitForUrl(String url, long timeout, TimeUnit timeoutUnit, String appName) throws InterruptedException {
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

	// TODO make this dependent on container mode
	private static final AiDockerClient docker = AiDockerClient.createLinuxClient();

	// ***** below here is clean ***** FIXME remove this

	// framework methods

	protected <T extends Domain> T getTelemetryTypeData(int index, String data){		
        Envelope mEnvelope = mockedIngestion.getItemsByType(data).get(index);
        Data<T> dHolder = (Data<T>) mEnvelope.getData();
        return dHolder.getBaseData();
    }
}