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

import org.junit.Assert;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;

public class AiDockerClient {
	
	public static String DEFAULT_WINDOWS_USER = "Administrator";
	public static String DEFAULT_WINDOWS_SHELL = "cmd";

	public static String DEFAULT_LINUX_USER = "root";
	public static String DEFAULT_LINUX_SHELL = "bash";

	public static final String DOCKER_EXE_ENV_VAR = "DOCKER_EXE";

	private String currentUser;
	private String shellExecutor;

	private String dockerExePath;

	public AiDockerClient(String user, String shellExecutor) {
		Preconditions.checkNotNull(user, "user");
		Preconditions.checkNotNull(shellExecutor, "shellExecutor");

		this.currentUser = user;
		this.shellExecutor = shellExecutor;

		// TODO also check a property
		String dockerPath = System.getenv(DOCKER_EXE_ENV_VAR);
		if (Strings.isNullOrEmpty(dockerPath)) {
			throw new RuntimeException(DOCKER_EXE_ENV_VAR+" not set");
		}
		File de = new File(dockerPath);
		if (!de.exists()) {
			throw new RuntimeException(String.format("Could not find docker: %s=%s", DOCKER_EXE_ENV_VAR, dockerPath));
		}
		this.dockerExePath = de.getAbsolutePath();
	}

	public String getCurrentUser() {
		return this.currentUser;
	}

	public String getShellExecutor() {
		return this.shellExecutor;
	}

	public static AiDockerClient createLinuxClient() {
		return new AiDockerClient(DEFAULT_LINUX_USER, DEFAULT_LINUX_SHELL);
	}

	public static AiDockerClient createWindowsClient() {
		return new AiDockerClient(DEFAULT_WINDOWS_USER, DEFAULT_WINDOWS_SHELL);
	}

	private ProcessBuilder buildProcess(String... cmdLine) {
		return new ProcessBuilder(cmdLine).redirectErrorStream(true);
	}

	private ProcessBuilder buildProcess(List<String> cmdLine) {
		return new ProcessBuilder(cmdLine).redirectErrorStream(true);
	}

	public String startContainer(String image, String portMapping, String agentMode) throws IOException, InterruptedException {
		Preconditions.checkNotNull(image, "image");
		Preconditions.checkNotNull(portMapping, "portMapping");

		String localIp = InetAddress.getLocalHost().getHostAddress();
		List<String> cmd = new ArrayList<>(Arrays.asList(dockerExePath, "run", "-d", "-p", portMapping, "--add-host=fakeingestion:"+localIp));
		if (agentMode != null) {
			cmd.add("--env");
			cmd.add("AI_AGENT_MODE="+agentMode);
		}
		cmd.add(image);
		Process p = buildProcess(cmd).start();
		if (!p.waitFor(10, TimeUnit.SECONDS)) {
			p.destroyForcibly();
			flushStdout(p);
			throw new RuntimeException("Could not start container: timeout reached");
		}
		final int exitCode = p.exitValue();
		if (exitCode != 0) {
			flushStdout(p);
			throw new RuntimeException("Starting container exited with code "+exitCode);
		}
		List<String> lines = CharStreams.readLines(new InputStreamReader(p.getInputStream()));
		return lines.get(0);
	}

	private static void flushStdout(Process p) throws IOException {
		Preconditions.checkNotNull(p);
		
		try (Scanner r = new Scanner(p.getInputStream())) {	
			while (r.hasNext()) {
				System.out.println(r.nextLine());
			}
		}
	}

	public void copyAndDeployToContainer(String id, File appArchive) throws IOException, InterruptedException {
		Preconditions.checkNotNull(id, "id");
		Preconditions.checkNotNull(appArchive, "appArchive");
		
		Process p = buildProcess(dockerExePath, "cp", appArchive.getAbsolutePath(), String.format("%s:%s", id, "/root/docker-stage")).start();
		waitAndCheckCodeForProcess(p, 10, TimeUnit.SECONDS, String.format("copy %s to container %s", appArchive.getPath(), id));
		
		execOnContainer(id, getShellExecutor(), "./deploy.sh", appArchive.getName());
	}

	public void execOnContainer(String id, String cmd, String... args) throws IOException, InterruptedException {
		Preconditions.checkNotNull(id, "id");
		Preconditions.checkNotNull(cmd, "cmd");

		List<String> cmdList = new ArrayList<>();
		cmdList.addAll(Arrays.asList(new String[]{dockerExePath, "container", "exec", id, cmd}));
		if (args.length > 0) {
			cmdList.addAll(Arrays.asList(args));
		}
		Process p = buildProcess(cmdList).start();
		try {
			waitAndCheckCodeForProcess(p, 50, TimeUnit.SECONDS, String.format("executing command on container: '%s'", id, Joiner.on(' ').join(cmdList)));
			flushStdout(p);
		}
		catch (Exception e) {
			throw e;
		}
	}

	private static void waitAndCheckCodeForProcess(Process p, long timeout, TimeUnit unit, String actionName) throws IOException, InterruptedException {
		if (!p.waitFor(timeout, unit)) {
			p.destroyForcibly();
			flushStdout(p);
			if (!Strings.isNullOrEmpty(actionName)) {
				throw new RuntimeException("Hit timeout trying to "+actionName);
			}
			throw new RuntimeException("Process timed out");
		}
		if (p.exitValue() != 0) {
			flushStdout(p);
			if (!Strings.isNullOrEmpty(actionName)) {
				throw new RuntimeException(String.format("Nonzero exit code (%d) trying to %s", p.exitValue(), actionName));
			}
			throw new RuntimeException("Nonzero exit code: "+p.exitValue());
		}
	}

	public void printContainerLogs(String containerId) throws IOException {
		Preconditions.checkNotNull(containerId, "containerId");

		Process p = buildProcess(dockerExePath, "container", "logs", containerId).start();
		flushStdout(p);
	}

	public void stopContainer(String id) throws IOException, InterruptedException {
		Process p = buildProcess(dockerExePath, "container", "stop", id).start();
		waitAndCheckCodeForProcess(p, 30, TimeUnit.SECONDS, String.format("stopping container %s", id));
	}

	public boolean isContainerRunning(String id) throws IOException, InterruptedException {
		Process p = new ProcessBuilder(dockerExePath, "inspect", "-f", "{{.State.Running}}", id).start();
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