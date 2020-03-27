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

    private static final int ITERATIONS = 10_000L;
    private static IpaEtwEventBase PROTOTYPE = new IpaInfo();
    static {
        PROTOTYPE.setAppName("EtwProvider-tests");
        PROTOTYPE.setExtensionVersion("fake-version");
        PROTOTYPE.setInstrumentationKey(UUID.randomUUID().toString());
        PROTOTYPE.setSubscriptionId(UUID.randomUUID().toString());
        PROTOTYPE.setResourceType("local-tests");
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
        EtwProvider ep = new EtwProvider();
        // ep.info("test", "testing %d!!!", 123);
        // ep.error("test-error", new Exception("exception message"), "The error: %s", "error test");
        // ep.error("test-error-no-exception", "The error: %s, test %d!", "no exception", 123);
        // ep.critical("test-critical", new Exception("critical message"), "this is critical: %d", -12354);
        // ep.critical("test-critical-noex", "this is critical raised w/o exception: %x", 6738921);

        IpaInfo einfo = createInfo("test.info.logger", "testDllExtracted", "test message %s", "hello!");
        IpaError eerror = createError("test.error.logger", "testDllExtracted", new Exception("test error exception"),"test error message '%s'", "hello again!");
        IpaWarn ewarn = createWarn("test.warn.logger", null, null, "simple warning: %s - %x", "NO EXCEPTION", 1234);
        IpaCritical ecritical = createCritical("test.critical.logger", "testDllExtracted.critical", new Error("test critical error"), "something very bad happened...%s %s", "but it's ok,", "this is only a test!!");
        ep.writeEvent(einfo);
        ep.writeEvent(eerror);
        ep.writeEvent(ewarn);
        ep.writeEvent(ecritical);
    }

    @Test
    public void testEventsOnLoop() throws Exception {
        int warnChance = 10;
        int errorChance = 5;
        int criticalChance = 25;
        EtwProvider ep = new EtwProvider();
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.currentTimeMillis();
            ep.writeEvent(createInfo("test.info", "testEventsOnLoop", "i=%d", i));
            int ecount = 1;
            System.out.println("Wrote info " + i);
            if (RandomUtils.nextInt(0, warnChance) == 0) {
                Throwable exception = null;
                if (RandomUtils.nextBoolean()) {
                    exception = new Exception(String.format("Exeption %d", i));
                }
                ep.writeEvent(createWarn("test.warn", "testEventsOnLoop", exception, "i=%d", i));
                ecount++;
                System.out.println("Wrote warn " + i + " " + (exception == null ? "" : " with exception"));
            }
            if (RandomUtils.nextInt(0, errorChance) == 0) {
                Throwable exception = null;
                if (RandomUtils.nextBoolean()) {
                    exception = new Exception(String.format("Exeption %d", i));
                }
                ep.writeEvent(createError("test.error", "testEventsOnLoop", exception, "i=%d", i));
                ecount++;
                System.out.println("Wrote error " + i + " " + (exception == null ? "" : " with exception"));
            }
            if (RandomUtils.nextInt(0, criticalChance) == 0) {
                Throwable exception = null;
                if (RandomUtils.nextBoolean()) {
                    exception = new Exception(String.format("Exeption %d", i));
                }
                ep.writeEvent(createCritical("test.critical", "testEventsOnLoop", exception, "i=%d", i));
                ecount++;
                System.out.println("Wrote critical " + i + " " + (exception == null ? "" : " with exception"));
            }
            System.out.println("Wrote " + ecount + " events in " + (System.currentTimeMillis() - start) + "ms");
        }
    }


}