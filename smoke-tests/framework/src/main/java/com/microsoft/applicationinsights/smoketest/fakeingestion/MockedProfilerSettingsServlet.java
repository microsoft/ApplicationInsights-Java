// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.fakeingestion;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MockedProfilerSettingsServlet extends HttpServlet {

  private static final Map<ProfilerState, String> CONFIGS;

  static {
    String now =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.ofHours(0)));

    CONFIGS = new HashMap<>();

    CONFIGS.put(
        ProfilerState.unconfigured,
        "{\n"
            + "   \"agentConcurrency\" : 0,\n"
            + "   \"collectionPlan\" : \"\",\n"
            + "   \"cpuTriggerConfiguration\" : \"--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled true\",\n"
            + "   \"defaultConfiguration\" : null,\n"
            + "   \"enabled\" : true,\n"
            + "   \"enabledLastModified\" : \"0001-01-01T00:00:00+00:00\",\n"
            + "   \"id\" : \"an-id\",\n"
            + "   \"lastModified\" : \"0001-01-01T00:00:00+00:00\",\n"
            + "   \"memoryTriggerConfiguration\" : \"--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled true\"\n"
            + "}\n");

    CONFIGS.put(
        ProfilerState.configuredEnabled,
        "{\n"
            + "   \"agentConcurrency\" : 0,\n"
            + "   \"collectionPlan\" : \"\",\n"
            + "   \"cpuTriggerConfiguration\" : \"--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled true\",\n"
            + "   \"defaultConfiguration\" : null,\n"
            + "   \"enabled\" : true,\n"
            + "   \"enabledLastModified\" : \""
            + now
            + "\",\n"
            + "   \"id\" : \"an-id\",\n"
            + "   \"lastModified\" : \""
            + now
            + "\",\n"
            + "   \"memoryTriggerConfiguration\" : \"--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled true\"\n"
            + "}\n");

    CONFIGS.put(
        ProfilerState.configuredDisabled,
        "{\n"
            + "   \"agentConcurrency\" : 0,\n"
            + "   \"collectionPlan\" : \"\",\n"
            + "   \"cpuTriggerConfiguration\" : \"--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled false\",\n"
            + "   \"defaultConfiguration\" : null,\n"
            + "   \"enabled\" : true,\n"
            + "   \"enabledLastModified\" : \""
            + now
            + "\",\n"
            + "   \"id\" : \"an-id\",\n"
            + "   \"lastModified\" : \""
            + now
            + "\",\n"
            + "   \"memoryTriggerConfiguration\" : \"--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled false\"\n"
            + "}\n");

    long expire = toSeconds(Instant.now().plusSeconds(100));

    CONFIGS.put(
        ProfilerState.manualprofile,
        "{\n"
            + "   \"agentConcurrency\" : 0,\n"
            + "   \"collectionPlan\" : \"--single --mode immediate --immediate-profiling-duration 120  --expiration "
            + expire
            + " --settings-moniker a-settings-moniker\",\n"
            + "   \"cpuTriggerConfiguration\" : \"--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled true\",\n"
            + "   \"defaultConfiguration\" : null,\n"
            + "   \"enabled\" : true,\n"
            + "   \"enabledLastModified\" : \""
            + now
            + "\",\n"
            + "   \"id\" : \"an-id\",\n"
            + "   \"lastModified\" : \""
            + now
            + "\",\n"
            + "   \"memoryTriggerConfiguration\" : \"--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled true\"\n"
            + "}\n");
  }

  private static long toSeconds(Instant time) {
    long offset = OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
    return (time.getEpochSecond() - offset) * 10000000L;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Optional<Map.Entry<ProfilerState, String>> entry =
        CONFIGS.entrySet().stream()
            .filter(
                it ->
                    ("/" + it.getKey().name() + "/api/profileragent/v4/settings")
                        .equals(req.getPathInfo()))
            .findFirst();

    if (entry.isPresent()) {
      resp.getWriter().append(entry.get().getValue());
    } else {
      resp.sendError(404, "Unknown URI");
    }
  }
}
