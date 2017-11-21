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
				return input.replace(':', '_');
			}
		});
	}

	
	
	@Parameter(0)
	public String appServer;
	
	@Parameter(1)
	public String os;
	
	@Parameter(2)
	public String jreVersion;
	// TODO application to deploy?

	private final static short BASE_PORT_NUMBER = 28080;
	private static short currentPortNumber = BASE_PORT_NUMBER;

	private static final String TEST_CONFIG_FILENAME = "testInfo.properties";

	@Rule
	public TestWatcher theWatchman = new TestWatcher() {
		@Override
		protected void failed(Throwable t, Description description) {
			// NOTE this happens after @After :)
			try {
				String containerId = lastContainerId();
				System.out.println("Test failure detected. Fetching container logs for "+containerId);
				docker.printContainerLogs(containerId);
				System.out.println("Fetching appserver logs");
				docker.execOnContainer(containerId, docker.getShellExecutor(), "tailLastLog.sh", "50");
			}
			catch (Exception e) {
				System.err.println("Error copying logs to stream");
				e.printStackTrace();
			}
		}
	};
	
	protected static Stack<ContainerInfo> containerStack = new Stack<>(); // FIXME make this a stack of container ids; push when start, pop to stop
	protected static String lastContainerId() {
		return containerStack.peek().getContainerId();
	}
	
	protected String warFileName;
	
	protected short extPort;
	protected String currentImageName;

	// TODO make this dependent on container mode
	protected static final AiDockerClient docker = AiDockerClient.createLinuxClient();

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

	protected static Runnable destroyAllContainers = new Runnable() {
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
		startMockedEndpoint();
		startDocker();
		System.out.println("Test preperation complete");
	}

	public void checkParams() {
		assertNotNull("appServer", this.appServer);
		assertNotNull("os", this.os);
		assertNotNull("jreVersion", this.jreVersion);
	}

	public void setupProperties() throws Exception {
		testProps.load(new FileReader(new File(Resources.getResource(TEST_CONFIG_FILENAME).toURI())));
		currentImageName = String.format("%s_%s_%s", this.appServer, this.os, this.jreVersion);
		extPort = currentPortNumber++;
	}

	public void startMockedEndpoint() throws Exception {
		mockedIngestion.startServer();
		TimeUnit.SECONDS.sleep(2);
		checkEndpointHealth();
	}

	private void checkEndpointHealth() throws Exception {
		// TODO make the port configurable
		String ok = HttpHelper.get("http://localhost:60606/");
		assertEquals(MockedAppInsightsIngestion.ENDPOINT_HEALTH_CHECK_RESPONSE, ok);
		String postResponse = HttpHelper.post("http://localhost:60606/v2/track", MockedAppInsightsIngestion.PING);
		assertEquals(MockedAppInsightsIngestion.PONG, postResponse);
	}

	public void startDocker() throws Exception {		
		System.out.printf("Starting container: %s%n", currentImageName);
		String containerId = docker.startContainer(currentImageName, extPort+":8080");
		assertFalse("'containerId' was null/empty attempting to start container: "+currentImageName, Strings.isNullOrEmpty(containerId));
		System.out.println("Container started: "+containerId);

		// TODO add health check for appserver

		ContainerInfo info = new ContainerInfo(containerId, currentImageName);
		try {
			warFileName = getProperty("ai.smoketest.testAppWarFile");
			System.out.printf("Deploying test application: %s...%n", warFileName);
			docker.copyAndDeployToContainer(containerId, new File(Resources.getResource(warFileName).toURI()));
			System.out.println("Test application deployed.");
		}
		catch (Exception e) {
			System.err.println("Error deploying test application. stopping container");
			// FIXME this could be done using a "fire-and-forget" thread
			stopContainer(info);
			throw e;
		}
		containerStack.push(info);
		// TODO start application dependencies---container(s)
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("Cleaning up test resources...");
		resetMockedIngestion();
		System.out.println("Test resources cleaned.");
	}

	public static void stopContainer(ContainerInfo info) throws Exception {	
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

	public void resetMockedIngestion() throws Exception {
		mockedIngestion.stopServer();
		mockedIngestion.resetData();
	}

	protected String getProperty(String key) {
		String rval = testProps.getProperty(key);
		if (rval == null) throw new RuntimeException(String.format("test property not found '%s'", key));
		return rval;
	}

	protected void waitForApp(String url, long timeout, TimeUnit timeoutUnit) throws InterruptedException {
		String rval = null;
		Stopwatch watch = Stopwatch.createStarted();
		do {
			if (watch.elapsed(timeoutUnit) > timeout) {
				throw new RuntimeException("Timeout reached waiting for test app to come online");
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
		assertFalse("Empty response. Test apps should return something non-empty for GET /<app-path>/", rval.isEmpty());
	}
}