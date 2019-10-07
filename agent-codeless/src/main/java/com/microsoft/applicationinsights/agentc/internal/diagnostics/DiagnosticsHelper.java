package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.nio.file.Files;

public class DiagnosticsHelper {
    private DiagnosticsHelper() {}

    private static File agentJarFile;

    @VisibleForTesting
    static volatile boolean appService;

    public static synchronized void setAgentJarFile(File agentJarFile) {
        DiagnosticsHelper.agentJarFile = agentJarFile;
        appService = Files.exists(agentJarFile.toPath().resolveSibling("appsvc.codeless"));
    }

    public static synchronized boolean isAppService() {
        return appService;
    }

}
