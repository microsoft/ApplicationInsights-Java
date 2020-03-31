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
package com.microsoft.applicationinsights.internal.etw;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.*;

import ch.qos.logback.classic.spi.ThrowableProxy;

import static org.junit.Assert.*;

import java.io.File;
import java.util.UUID;

import com.microsoft.applicationinsights.internal.etw.events.IpaCritical;
import com.microsoft.applicationinsights.internal.etw.events.IpaError;
import com.microsoft.applicationinsights.internal.etw.events.IpaInfo;
import com.microsoft.applicationinsights.internal.etw.events.IpaWarn;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventErrorBase;

public class EtwProviderTests {
    private static final File dllTempFolder = DllFileUtils.buildDllLocalPath();

    @BeforeClass
    public static void cleanTempFolder() {
        if (dllTempFolder.exists()) {
            System.out.println("Cleaning temp folder: "+dllTempFolder.getAbsolutePath());
            for (File f : dllTempFolder.listFiles()) {
                if(!f.delete()) {
                    System.err.println("Could not delete "+f);
                }
            }
            if (!dllTempFolder.delete()) {
                System.err.println("Could not delete "+dllTempFolder.getAbsolutePath());
            } else {
                System.out.println("Deleted "+dllTempFolder.getAbsolutePath());
            }
        }
    }

    private static IpaEtwEventBase PROTOTYPE = new IpaInfo();
    private static final long EVENT_STATS_TIMER_PERIOD_MILLISECONDS;
    static {
        PROTOTYPE.setAppName("EtwProvider-tests");
        PROTOTYPE.setExtensionVersion("fake-version");
        PROTOTYPE.setInstrumentationKey(UUID.randomUUID().toString());
        PROTOTYPE.setSubscriptionId(UUID.randomUUID().toString());
        PROTOTYPE.setResourceType("local-tests");

        String speriod = System.getProperty("ai.tests.etw.stats.period");
        long period = 2000; // default 2 seconds.
        if (speriod != null) {
            try {
                period = Long.parseLong(speriod);
            } catch(Exception e) {
                // ignore
            }
        }
        EVENT_STATS_TIMER_PERIOD_MILLISECONDS = period;
    }

    private IpaInfo createInfo(String logger, String operation, String messageFormat, Object...messageArgs) {
        IpaInfo rval = new IpaInfo(PROTOTYPE);
        rval.setLogger(logger);
        rval.setMessageFormat(messageFormat);
        rval.setMessageArgs(messageArgs);
        return rval;
    }

    private IpaError createError(String logger, String operation, Throwable throwable, String messageFormat, Object...messageArgs) {
        IpaError rval = new IpaError(PROTOTYPE);
        populateEventWithException(rval, logger, operation, throwable, messageFormat, messageArgs);
        return rval;
    }

    private IpaWarn createWarn(String logger, String operation, Throwable throwable, String messageFormat, Object...messageArgs) {
        IpaWarn rval = new IpaWarn(PROTOTYPE);
        populateEventWithException(rval, logger, operation, throwable, messageFormat, messageArgs);
        return rval;
    }

    private IpaCritical createCritical(String logger, String operation, Throwable throwable, String messageFormat, Object...messageArgs) {
        IpaCritical rval = new IpaCritical(PROTOTYPE);
        populateEventWithException(rval, logger, operation, throwable, messageFormat, messageArgs);
        return rval;
    }

    private void populateEventWithException(IpaEtwEventErrorBase event, String logger, String operation, Throwable throwable, String messageFormat, Object...messageArgs) {
        event.setLogger(logger);
        event.setOperation(operation);
        if (throwable != null) {
            event.setStacktrace(new ThrowableProxy(throwable));
        }
        event.setMessageFormat(messageFormat);
        event.setMessageArgs(messageArgs);
    }

    @Before
    public void checkOs() {
        Assume.assumeTrue("Ignoring etw test. Not on windows", SystemUtils.IS_OS_WINDOWS);
    }

    @Test
    public void testDllExtracted() throws Exception {
        String filename = EtwProvider.getDllFilenameForArch();
        final File dllPath = new File(dllTempFolder, filename);
        System.out.println("Checking for DLL: "+dllPath.getAbsolutePath());
        assertTrue("Dll does not exist: "+dllPath.getAbsolutePath(), dllPath.exists());

        IpaInfo einfo = createInfo("test.info.logger", "testDllExtracted", "test message %s", "hello!");
        IpaError eerror = createError("test.error.logger", "testDllExtracted", new Exception("test error exception"),"test error message '%s'", "hello again!");
        IpaWarn ewarn = createWarn("test.warn.logger", null, null, "simple warning: %s - %x", "NO EXCEPTION", 1234);
        IpaCritical ecritical = createCritical("test.critical.logger", "testDllExtracted.critical", new Error("test critical error"), "something very bad happened...%s %s", "but it's ok,", "this is only a test!!");

        EtwProvider ep = new EtwProvider();
        ep.writeEvent(einfo);
        ep.writeEvent(eerror);
        ep.writeEvent(ewarn);
        ep.writeEvent(ecritical);
    }

    private void longTestCheck() {
        Assume.assumeFalse("Long tests disabled", "true".equalsIgnoreCase(System.getProperty("ai.tests.etw.long.disabled")));
        Assume.assumeTrue("Not using release build. Skipping testEventsOnLoop", "release".equalsIgnoreCase(System.getProperty("ai.etw.native.build")));
    }

    @Test
    public void testEventsOnLoop_10k() throws Exception {
        longTestCheck();
        runLoopTest(10_000);
    }

    @Test
    public void testEventsOnLoop_100k() throws Exception {
        longTestCheck();
        runLoopTest(100_000);
    }

    private static class EventCounts {
        int info = 0;
        int warn = 0;
        int error = 0;
        int critical = 0;
        int sum() {
            return info + warn + error + critical;
        }

        @Override
        public String toString() {
            return String.format("{ info: %d, warn: %d, error: %d, critical: %d }");
        }

        void plus(EventCounts operand) {
            info += operand.info;
            warn += operand.warn;
            error += operand.error;
            critical += operand.critical;
        }
    }

    private void runLoopTest(int iterations) throws Exception {
        int warnChance = 10;
        int errorChance = 5;
        int criticalChance = 25;
        long methodStart = System.currentTimeMillis();
        EtwProvider ep = new EtwProvider();
        EventCounts totalEvents = new EventCounts();
        long printTimer = 0;
        EventCounts accumulator = new EventCounts();
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            ep.writeEvent(createInfo("test.info", "testEventsOnLoop", "i=%d", i));
            accumulator.info++;
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
            totalEvents.plus(accumulator);
            if (printTimer >= EVENT_STATS_TIMER_PERIOD_MILLISECONDS) {
                System.out.println("Wrote " + accumulator.sum() + " events "+accumulator.toString()+" in " + printTimer + "ms "+String.format("(avg=%.3fms)", ((double)printTimer/accumulator.sum())));
                printTimer = 0;
                accumulator = new EventCounts();
            }
        }
        long totalElapsedTime = System.currentTimeMillis()-methodStart;
        System.out.println("FINAL STATS: wrote "+totalEvents.sum()+" events "+totalEvents.toString()+" in "+totalElapsedTime+"ms "+String.format("(avg=%.3fms)", ((double)totalElapsedTime/totalEvents.sum())));
    }
}