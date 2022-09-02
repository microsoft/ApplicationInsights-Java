// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.status;

import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.status.StatusFile.initLogDir;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.AgentExtensionVersionFinder;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsTestHelper;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@Disabled("failing on CI when deleted the @TempDir")
@ExtendWith(SystemStubsExtension.class)
class StatusFileTests {

  @TempDir File tempFolder;

  @SystemStub EnvironmentVariables envVars = new EnvironmentVariables();

  private static final String TEST_IKEY = "fake-ikey-123";
  private static final String FAKE_VERSION = "0.0.1-test";

  @BeforeEach
  void setup() {
    // TODO these tests currently only pass on windows
    assumeTrue(DiagnosticsHelper.isOsWindows());
    envVars.set("APPINSIGHTS_INSTRUMENTATIONKEY", TEST_IKEY);
    envVars.set(
        AgentExtensionVersionFinder.AGENT_EXTENSION_VERSION_ENVIRONMENT_VARIABLE, FAKE_VERSION);
  }

  @AfterEach
  void resetStaticVariables() {
    DiagnosticsTestHelper.reset();
  }

  @Test
  void defaultDirectoryIsCorrect() {
    // TODO this test doesn't pass inside of windows + bash because bash sets HOME env
    assumeTrue(System.getenv(StatusFile.HOME_ENV_VAR) == null);
    assertThat(initLogDir()).isEqualTo("./LogFiles/ApplicationInsights");
  }

  @Test
  void siteLogDirPropertyUpdatesBaseDir() {
    String parentDir = "/temp/test/prop";
    System.setProperty("site.logdir", parentDir);
    assertThat(StatusFile.initLogDir()).isEqualTo("/temp/test/prop/ApplicationInsights");
  }

  @Test
  void homeEnvVarUpdatesBaseDir() {
    String homeDir = "/temp/test";
    envVars.set(StatusFile.HOME_ENV_VAR, homeDir);
    assertThat(StatusFile.initLogDir()).isEqualTo("/temp/test/LogFiles/ApplicationInsights");
  }

  @Test
  void siteLogDirHasPrecedenceOverHome() {
    String homeDir = "/this/is/wrong";
    envVars.set(StatusFile.HOME_ENV_VAR, homeDir);
    System.setProperty("site.logdir", "/the/correct/dir");
    assertThat(StatusFile.initLogDir()).isEqualTo("/the/correct/dir/ApplicationInsights");
  }

  @Test
  void mapHasExpectedValues() {
    Map<String, Object> jsonMap = StatusFile.getJsonMap();

    assertMapHasExpectedInformation(jsonMap);
  }

  void assertMapHasExpectedInformation(Map<String, Object> inputMap) {
    assertMapHasExpectedInformation(inputMap, null, null);
  }

  void assertMapHasExpectedInformation(
      Map<String, Object> inputMap, @Nullable String key, @Nullable String value) {
    int size = 5;
    if (key != null && value != null) {
      size = 6;
      assertThat(inputMap).containsEntry(key, value);
    }
    assertThat(inputMap).hasSize(size);
    assertThat(inputMap).containsKey("MachineName");
    assertThat(inputMap).containsEntry("Ikey", TEST_IKEY);
    assertThat(inputMap).containsKey("PID");
    assertThat(inputMap).containsEntry("AppType", "java");
    assertThat(inputMap).containsEntry("ExtensionVersion", FAKE_VERSION);
  }

  @Test
  void connectionStringWorksToo() {
    String ikey = "a-different-ikey-456789";
    envVars.set("APPLICATIONINSIGHTS_CONNECTION_STRING", "InstrumentationKey=" + ikey);
    Map<String, Object> jsonMap = StatusFile.getJsonMap();
    assertThat(jsonMap).containsEntry("Ikey", ikey);
  }

  @Test
  void writesCorrectFile() throws Exception {
    DiagnosticsTestHelper.setIsAppSvcAttachForLoggingPurposes(true);
    runWriteFileTest(true);
  }

  private void runWriteFileTest(boolean enabled) throws Exception {
    assertThat(tempFolder.isDirectory()).isTrue();
    assertThat(tempFolder.list()).isEmpty();

    StatusFile.directory = tempFolder.getAbsolutePath();
    StatusFile.write();
    pauseForFileWrite();

    if (enabled) {
      assertThat(tempFolder.list()).hasSize(1);
      Map map = parseJsonFile(tempFolder);
      assertMapHasExpectedInformation(map);
    } else {
      assertThat(tempFolder.list()).isEmpty();
    }
  }

  private static void pauseForFileWrite() throws InterruptedException {
    TimeUnit.SECONDS.sleep(5);
  }

  Map parseJsonFile(File tempFolder) throws IOException {
    JsonAdapter<Map> adapter = new Moshi.Builder().build().adapter(Map.class);
    String fileName = StatusFile.constructFileName(StatusFile.getJsonMap());
    String contents =
        new String(Files.readAllBytes(new File(tempFolder, fileName).toPath()), UTF_8);
    return adapter.fromJson(contents);
  }

  @Test
  void doesNotWriteIfNotAppService() throws Exception {
    DiagnosticsTestHelper.setIsAppSvcAttachForLoggingPurposes(false); // just to be sure

    StatusFile.directory = tempFolder.getAbsolutePath();
    assertThat(tempFolder.isDirectory()).isTrue();
    assertThat(tempFolder.list()).isEmpty();
    StatusFile.write();
    pauseForFileWrite();
    assertThat(tempFolder.list()).isEmpty();
    StatusFile.putValueAndWrite("shouldNot", "write");
    pauseForFileWrite();
    assertThat(tempFolder.list()).isEmpty();
  }

  @Test
  void putValueAndWriteOverwritesCurrentFile() throws Exception {
    String key = "write-test";
    try {
      DiagnosticsTestHelper.setIsAppSvcAttachForLoggingPurposes(true);

      StatusFile.directory = tempFolder.getAbsolutePath();
      assertThat(tempFolder.isDirectory()).isTrue();
      assertThat(tempFolder.list()).isEmpty();
      StatusFile.write();
      pauseForFileWrite();
      assertThat(tempFolder.list()).hasSize(1);
      Map map = parseJsonFile(tempFolder);
      assertMapHasExpectedInformation(map);

      String value = "value123";
      StatusFile.putValueAndWrite(key, value);
      pauseForFileWrite();
      assertThat(tempFolder.list()).hasSize(1);
      map = parseJsonFile(tempFolder);
      assertMapHasExpectedInformation(map, key, value);

    } finally {
      DiagnosticsTestHelper.setIsAppSvcAttachForLoggingPurposes(false);
      StatusFile.CONSTANT_VALUES.remove(key);
    }
  }

  @Test
  void fileNameHasMachineNameAndPid() {
    Map<String, Object> jsonMap = StatusFile.getJsonMap();
    String s = StatusFile.constructFileName(jsonMap);
    assertThat(s).startsWith(StatusFile.FILENAME_PREFIX);
    assertThat(s).endsWith(StatusFile.FILE_EXTENSION);
    assertThat(s).contains(jsonMap.get("MachineName").toString());
    assertThat(s).contains(jsonMap.get("PID").toString());
  }
}
