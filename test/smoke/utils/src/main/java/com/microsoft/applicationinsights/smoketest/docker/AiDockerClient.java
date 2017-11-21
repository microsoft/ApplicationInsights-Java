package com.microsoft.applicationinsights.smoketest.docker;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.ArrayUtil;
import org.junit.Assert;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;

public class AiDockerClient {
	
	public static String DEFAULT_WINDOWS_USER = "Administrator";
	public static String DEFAULT_LINUX_USER = "root";

	private String currentUser;

	public AiDockerClient() {
		currentUser = "root";
	}

	public AiDockerClient(String user) {
		this.currentUser = user;
	}

	public String getCurrentUser() {
		return this.currentUser;
	}

	public static AiDockerClient createLinuxClient() {
		return new AiDockerClient(DEFAULT_LINUX_USER);
	}

	public static AiDockerClient createWindowsClient() {
		return new AiDockerClient(DEFAULT_WINDOWS_USER);
	}

	public String startContainer(String image, String portMapping) throws IOException, InterruptedException {
		Preconditions.checkNotNull(image, "image");
		Preconditions.checkNotNull(portMapping, "portMapping");

		String localIp = InetAddress.getLocalHost().getHostAddress();

		Process p = new ProcessBuilder("docker", "run", "-d", "-p", portMapping, "--add-host=fakeingestion:"+localIp, image).start();
		if (!p.waitFor(3, TimeUnit.SECONDS)) {
			p.destroyForcibly();
			flushStandardStreams(p);
			throw new RuntimeException("Could not start container: timeout reached");
		}
		final int exitCode = p.exitValue();
		if (exitCode != 0) {
			flushStandardStreams(p);
			throw new RuntimeException("Starting container exited with code "+exitCode);
		}
		List<String> lines = CharStreams.readLines(new InputStreamReader(p.getInputStream()));
		return lines.get(0);
	}

	private static void flushStandardStreams(Process p) throws IOException {
		Preconditions.checkNotNull(p);
		CharStreams.copy(new InputStreamReader(p.getInputStream()), System.out);
		CharStreams.copy(new InputStreamReader(p.getErrorStream()), System.err);
	}

	public void copyAndDeployToContainer(String id, File appArchive) throws IOException, InterruptedException {
		Preconditions.checkNotNull(id, "id");
		Preconditions.checkNotNull(appArchive, "appArchive");
		
		Process p = new ProcessBuilder("docker", "cp", appArchive.getAbsolutePath(), String.format("%s:%s", id, "/root/docker-stage")).start();
		waitAndCheckCodeForProcess(p, 10, TimeUnit.SECONDS, String.format("copy %s to container %s", appArchive.getPath(), id));
		// TODO chmod and chown; maybe
		
		execOnContainer(id, "bash", "./deploy.sh", appArchive.getName());
	}

	public void execOnContainer(String id, String cmd, String... args) throws IOException, InterruptedException {
		Preconditions.checkNotNull(id, "id");
		Preconditions.checkNotNull(cmd, "cmd");

		List<String> cmdList = new ArrayList<>();
		cmdList.addAll(Arrays.asList(new String[]{"docker", "container", "exec", "-d", "-u", getCurrentUser(), id, cmd}));
		if (args.length > 0) {
			cmdList.addAll(Arrays.asList(args));
		}
		Process p = new ProcessBuilder(cmdList).start();
		try {
			waitAndCheckCodeForProcess(p, 10, TimeUnit.SECONDS, String.format("executing command on container: '%s'", id, Joiner.on(' ').join(cmdList)));
			flushStandardStreams(p);
		}
		catch (Exception e) {
			throw e;
		}
		
	}

	private static void waitAndCheckCodeForProcess(Process p, long timeout, TimeUnit unit, String actionName) throws IOException, InterruptedException {
		if (!p.waitFor(timeout, unit)) {
			p.destroyForcibly();
			flushStandardStreams(p);
			if (!Strings.isNullOrEmpty(actionName)) {
				throw new RuntimeException("Hit timeout trying to "+actionName);
			}
			throw new RuntimeException("Process timed out");
		}
		if (p.exitValue() != 0) {
			flushStandardStreams(p);
			if (!Strings.isNullOrEmpty(actionName)) {
				throw new RuntimeException(String.format("Nonzero exit code (%d) trying to %s", p.exitValue(), actionName));
			}
			throw new RuntimeException("Nonzero exit code: "+p.exitValue());
		}
	}

	public void copyLogsToStream(String containerId, Appendable stdout, Appendable stderr) throws IOException, InterruptedException {
		Preconditions.checkNotNull(containerId, "containerId");
		Preconditions.checkNotNull(stdout, "stdout");
		Preconditions.checkNotNull(stderr, "stderr");

		Process p = new ProcessBuilder("docker", "container", "logs", containerId).start();
		waitAndCheckCodeForProcess(p, 20, TimeUnit.SECONDS, "docker container logs");
		CharStreams.copy(new InputStreamReader(p.getInputStream()), stdout);
		CharStreams.copy(new InputStreamReader(p.getErrorStream()), stderr);
	}

	public void printContainerLogs(String containerId) throws IOException {
		Preconditions.checkNotNull(containerId, "containerId");

		Process p = new ProcessBuilder("docker", "container", "logs", containerId).redirectErrorStream(true).start();
		try (Scanner r = new Scanner(p.getInputStream())) {	
			while (r.hasNext()) {
				System.out.println(r.nextLine());
			}
		}
	}

	public void stopContainer(String id) throws IOException, InterruptedException {
		Process p = new ProcessBuilder("docker", "container", "stop", id).start();
		waitAndCheckCodeForProcess(p, 30, TimeUnit.SECONDS, 
			String.format("stopping container %s", id));
	}

	public boolean isContainerRunning(String id) throws IOException, InterruptedException {
		Process p = new ProcessBuilder("docker", "inspect", "-f", "{{.State.Running}}", id).start();
		waitAndCheckCodeForProcess(p, 1, TimeUnit.SECONDS, String.format("checking if container is running: %s", id));
		
		StringWriter sw = new StringWriter();
		try {
			CharStreams.copy(new InputStreamReader(p.getInputStream()), sw);
			System.out.printf("Checking for running container %s: %s%n", id, sw.toString());
			return Boolean.parseBoolean(sw.toString());
		}
		finally {
			sw.close();
		}
	}
}