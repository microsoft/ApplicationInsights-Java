package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

public class ResourceIdFinder extends CachedDiagnosticsValueFinder {
    @VisibleForTesting
    static final String WEBSITE_HOSTNAME_ENV_VAR = "WEBSITE_HOSTNAME";
    @VisibleForTesting
    static final String DIAGNOSTIC_LOGS_MOUNT_PATH_ENV_VAR = "DIAGNOSTIC_LOGS_MOUNT_PATH";
    @VisibleForTesting
    static final String RESOURCE_ID_FILE_NAME = "resourceid";
    @VisibleForTesting
    static final String RESOURCE_ID_FIELD_NAME = "resourceId";

    @Override
    public String getName() {
        return RESOURCE_ID_FIELD_NAME;
    }

    @Override
    protected String populateValue() {
        String diagLogMountPath = System.getenv(ResourceIdFinder.DIAGNOSTIC_LOGS_MOUNT_PATH_ENV_VAR);

        if (!Strings.isNullOrEmpty(diagLogMountPath)) {
            try {
                String resId = new String(Files.readAllBytes(Paths.get(diagLogMountPath, ResourceIdFinder.RESOURCE_ID_FILE_NAME)), StandardCharsets.UTF_8);
                if (!resId.isEmpty()) {
                    return resId;
                }
            } catch (Exception e) {
                // nop. this can't be logged since we're inside the logger layout class
            }
        }

        // FUTURE: WEBSITE_HOSTNAME is known to be inaccurate due to the need to not recycle an app after a swap is done.
        return System.getenv(ResourceIdFinder.WEBSITE_HOSTNAME_ENV_VAR);
    }
}
