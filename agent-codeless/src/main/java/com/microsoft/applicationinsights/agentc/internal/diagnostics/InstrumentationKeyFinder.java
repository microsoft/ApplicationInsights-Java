package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.agentc.internal.model.Global;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Keywords;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class InstrumentationKeyFinder implements DiagnosticsValueFinder {
    @Nonnull
    @Override
    public String getName() {
        return "ikey";
    }

    @Nullable
    @Override
    public String getValue() {
        final String ikey = Global.getInstrumentationKey();
        if (ikey != null) {
            return ikey;
        }

        final String connStr = System.getenv(TelemetryConfigurationFactory.CONNECTION_STRING_ENV_VAR_NAME);
        if (!Strings.isNullOrEmpty(connStr)) {
            try {
                // see ConnectionString.parseInto
                Map<String, String> kvps = new HashMap<>(Splitter.on(';').trimResults().omitEmptyStrings().withKeyValueSeparator('=').split(connStr));
                return kvps.get(Keywords.INSTRUMENTATION_KEY);
            } catch (Exception e) {
                return null;
            }
        }
        return System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY");
    }
}
