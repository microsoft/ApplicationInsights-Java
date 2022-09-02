// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.IpaCritical;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.IpaError;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.IpaInfo;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.IpaVerbose;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.IpaWarn;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventErrorBase;
import java.io.File;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("SystemOut")
class EtwProviderTests {
  private static final String FOLDER_NAME = "EtwProviderTests";
  private static final File dllTempFolder = DllFileUtils.buildDllLocalPath(FOLDER_NAME);

  @BeforeAll
  static void cleanTempFolder() {
    if (dllTempFolder.exists()) {
      System.out.println("Cleaning temp folder: " + dllTempFolder.getAbsolutePath());
      for (File f : dllTempFolder.listFiles()) {
        if (!f.delete()) {
          System.err.println("Could not delete " + f);
        }
      }
      if (!dllTempFolder.delete()) {
        System.err.println("Could not delete " + dllTempFolder.getAbsolutePath());
      } else {
        System.out.println("Deleted " + dllTempFolder.getAbsolutePath());
      }
    }
  }

  private static final IpaEtwEventBase PROTOTYPE = new IpaInfo();
  private static final long EVENT_STATS_TIMER_PERIOD_MILLISECONDS;

  static {
    PROTOTYPE.setAppName("EtwProvider-tests");
    PROTOTYPE.setExtensionVersion("fake-version");
    PROTOTYPE.setSubscriptionId(UUID.randomUUID().toString());

    String speriod = System.getProperty("ai.tests.etw.stats.period");
    long period = 2000; // default 2 seconds.
    if (speriod != null) {
      period = Long.parseLong(speriod);
    }
    EVENT_STATS_TIMER_PERIOD_MILLISECONDS = period;
  }

  private static IpaVerbose createVerbose(
      String logger, String operation, String messageFormat, Object... messageArgs) {
    IpaVerbose rval = new IpaVerbose(PROTOTYPE);
    rval.setLogger(logger);
    rval.setOperation(operation);
    rval.setMessageFormat(messageFormat);
    rval.setMessageArgs(messageArgs);
    return rval;
  }

  private static IpaInfo createInfo(
      String logger, String operation, String messageFormat, Object... messageArgs) {
    IpaInfo rval = new IpaInfo(PROTOTYPE);
    rval.setLogger(logger);
    rval.setOperation(operation);
    rval.setMessageFormat(messageFormat);
    rval.setMessageArgs(messageArgs);
    return rval;
  }

  private static IpaError createError(
      String logger,
      String operation,
      @Nullable Throwable throwable,
      String messageFormat,
      Object... messageArgs) {
    IpaError rval = new IpaError(PROTOTYPE);
    populateEventWithException(rval, logger, operation, throwable, messageFormat, messageArgs);
    return rval;
  }

  private static IpaWarn createWarn(
      String logger,
      String operation,
      @Nullable Throwable throwable,
      String messageFormat,
      Object... messageArgs) {
    IpaWarn rval = new IpaWarn(PROTOTYPE);
    populateEventWithException(rval, logger, operation, throwable, messageFormat, messageArgs);
    return rval;
  }

  private static IpaCritical createCritical(
      String logger,
      String operation,
      @Nullable Throwable throwable,
      String messageFormat,
      Object... messageArgs) {
    IpaCritical rval = new IpaCritical(PROTOTYPE);
    populateEventWithException(rval, logger, operation, throwable, messageFormat, messageArgs);
    return rval;
  }

  private static void populateEventWithException(
      IpaEtwEventErrorBase event,
      String logger,
      String operation,
      @Nullable Throwable throwable,
      String messageFormat,
      Object... messageArgs) {
    event.setLogger(logger);
    event.setOperation(operation);
    if (throwable != null) {
      event.setStacktrace(ExceptionUtils.getStackTrace(throwable));
    }
    event.setMessageFormat(messageFormat);
    event.setMessageArgs(messageArgs);
  }

  @BeforeEach
  void checkOs() {
    assumeTrue(SystemUtils.IS_OS_WINDOWS, "Ignoring etw test. Not on windows");
    assumeFalse(
        Boolean.parseBoolean(System.getProperty("skipWinNative")),
        "Ignoring etw test. skipWinNative=true");
  }

  @Test
  void testDllExtracted() throws Exception {
    new EtwProvider(FOLDER_NAME); // Triggers DLL extraction
    String filename = EtwProvider.getDllFilenameForArch();
    File dllPath = new File(dllTempFolder, filename);
    System.out.println("Checking for DLL: " + dllPath.getAbsolutePath());
    assertThat(dllPath).exists();

    IpaVerbose everbose =
        createVerbose(
            "test.verbose.logger", "testDllExtracted", "verbose test message %s", "hello, world!");
    IpaInfo einfo = createInfo("test.info.logger", "testDllExtracted", "test message %s", "hello!");
    IpaError eerror =
        createError(
            "test.error.logger",
            "testDllExtracted",
            new Exception("test error exception"),
            "test error message '%s'",
            "hello again!");
    IpaWarn ewarn =
        createWarn("test.warn.logger", null, null, "simple warning: %s - %x", "NO EXCEPTION", 1234);
    IpaCritical ecritical =
        createCritical(
            "test.critical.logger",
            "testDllExtracted.critical",
            new Error("test critical error"),
            "something very bad happened...%s %s",
            "but it's ok,",
            "this is only a test!!");

    EtwProvider ep = new EtwProvider(FOLDER_NAME);
    ep.writeEvent(everbose);
    ep.writeEvent(einfo);
    ep.writeEvent(eerror);
    ep.writeEvent(ewarn);
    ep.writeEvent(ecritical);
    IpaWarn otherWarn = createWarn("test.warn.logger2", "some-op", null, "another warning");
    otherWarn.setStacktrace("TEST STACKTRACE");
    ep.writeEvent(otherWarn);
  }

  private static void longTestCheck() {
    assumeFalse(
        "true".equalsIgnoreCase(System.getProperty("ai.etw.tests.long.disabled")),
        "Long tests disabled");
    assumeTrue(
        "release".equalsIgnoreCase(System.getProperty("ai.etw.native.build"))
            || !"true".equalsIgnoreCase(System.getProperty("ai.etw.native.verbose")),
        "Verbose output enabled. Skipping long tests.");
  }

  @Test
  void testEventsOnLoop_50k() throws Exception {
    longTestCheck();
    runLoopTest(50_000);
  }

  @Test
  void testEventsOnLoop_500k() throws Exception {
    longTestCheck();
    runLoopTest(500_000);
  }

  private static class EventCounts {
    int verbose = 0;
    int info = 0;
    int warn = 0;
    int error = 0;
    int critical = 0;

    int sum() {
      return info + warn + error + critical;
    }

    @Override
    public String toString() {
      return String.format(
          "{ verbose: %d, info: %d, warn: %d, error: %d, critical: %d }",
          verbose, info, warn, error, critical);
    }

    void plus(EventCounts operand) {
      verbose += operand.verbose;
      info += operand.info;
      warn += operand.warn;
      error += operand.error;
      critical += operand.critical;
    }

    void reset() {
      verbose = 0;
      info = 0;
      warn = 0;
      error = 0;
      critical = 0;
    }
  }

  private static void runLoopTest(int iterations) throws Exception {
    int verboseChance = 20;
    int warnChance = 10;
    int errorChance = 5;
    int criticalChance = 25;
    long methodStart = System.currentTimeMillis();
    EtwProvider ep = new EtwProvider(FOLDER_NAME);
    EventCounts totalEvents = new EventCounts();
    long printTimer = 0;
    EventCounts accumulator = new EventCounts();
    System.out.println("START: totalEvents: " + totalEvents.sum() + totalEvents);
    System.out.println("       accumulator: " + accumulator.sum() + accumulator);
    for (int i = 0; i < iterations; i++) {
      long start = System.currentTimeMillis();
      ep.writeEvent(createInfo("test.info", "testEventsOnLoop", "i=%d", i));
      accumulator.info++;
      if (RandomUtils.nextInt(0, verboseChance) == 0) {
        ep.writeEvent(createVerbose("test.verbose", "testEventsOnLoop", "i=%d", i));
        accumulator.verbose++;
      }
      if (RandomUtils.nextInt(0, warnChance) == 0) {
        Throwable exception = null;
        if (RandomUtils.nextBoolean()) {
          exception = new Exception(String.format("Exeption %d", i));
        }
        ep.writeEvent(createWarn("test.warn", "testEventsOnLoop", exception, "i=%d", i));
        accumulator.warn++;
      }
      if (RandomUtils.nextInt(0, errorChance) == 0) {
        Throwable exception = null;
        if (RandomUtils.nextBoolean()) {
          exception = new Exception(String.format("Exeption %d", i));
        }
        ep.writeEvent(createError("test.error", "testEventsOnLoop", exception, "i=%d", i));
        accumulator.error++;
      }
      if (RandomUtils.nextInt(0, criticalChance) == 0) {
        Throwable exception = null;
        if (RandomUtils.nextBoolean()) {
          exception = new Exception(String.format("Exeption %d", i));
        }
        ep.writeEvent(createCritical("test.critical", "testEventsOnLoop", exception, "i=%d", i));
        accumulator.critical++;
      }
      long elapsedTime = (System.currentTimeMillis() - start);
      printTimer += elapsedTime;
      if (printTimer >= EVENT_STATS_TIMER_PERIOD_MILLISECONDS) {
        totalEvents.plus(accumulator);
        System.out.println(
            "Wrote "
                + accumulator.sum()
                + " events ("
                + totalEvents.sum()
                + ") "
                + accumulator
                + " in "
                + printTimer
                + "ms "
                + String.format(
                    "(avg=%.3fms)", ((double) printTimer / (double) accumulator.sum())));
        printTimer = 0;
        accumulator.reset();
      }
    }
    totalEvents.plus(accumulator);
    long totalElapsedTime = System.currentTimeMillis() - methodStart;
    System.out.println(
        "FINAL STATS: wrote "
            + totalEvents.sum()
            + " events "
            + totalEvents
            + " in "
            + totalElapsedTime
            + "ms "
            + String.format(
                "(avg=%.3fms)", ((double) totalElapsedTime / (double) totalEvents.sum())));
  }
}
