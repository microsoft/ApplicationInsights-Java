package com.microsoft.applicationinsights.internal.etw;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;

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
    public void testDllExtracted() {
        // TODO assume windows
        String filename = EtwProvider.getDllFilenameForArch();
        final File dllPath = new File(dllTempFolder, filename);
        System.out.println("Checking for DLL: "+dllPath.getAbsolutePath());
        assertTrue("Dll does not exist: "+dllPath.getAbsolutePath(), dllPath.exists());
        EtwProvider ep = new EtwProvider();
        ep.info("test", "testing %d!!!", 123);
        ep.error("test-error", new Exception("exception message"), "The error: %s", "error test");
        ep.error("test-error-no-exception", "The error: %s, test %d!", "no exception", 123);
        ep.critical("test-critical", new Exception("critical message"), "this is critical: %d", -12354);
        ep.critical("test-critical-noex", "this is critical raised w/o exception: %x", 6738921);
    }

}