package com.microsoft.applicationinsights.internal.etw;

import org.junit.*;

import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyVO;

import static org.junit.Assert.*;

import java.io.File;

import com.microsoft.applicationinsights.internal.etw.events.IpaError;
import com.microsoft.applicationinsights.internal.etw.events.IpaInfo;

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

    @Test
    public void testDllExtracted() throws Exception {
        // TODO assume windows
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
        IpaInfo einfo = new IpaInfo();
        einfo.setLogger("test-info-logger");
        einfo.setMessageFormat("test message %s");
        einfo.setMessageArgs("hello!");
        IpaError eerror= new IpaError();
        eerror.setLogger("test-error-logger");
        eerror.setMessageFormat("test error message '%s'");
        eerror.setMessageArgs("hello again!");
        eerror.setStacktrace(new ThrowableProxy(new Exception("test exception")));
        ep.writeEvent(einfo);
        ep.writeEvent(eerror);
    }
}