package com.microsoft.applicationinsights.agent.internal;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.web.internal.correlation.InstrumentationKeyResolver;
import io.opentelemetry.auto.bootstrap.instrumentation.aiappid.AiAppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppIdSupplier implements AiAppId.Supplier {

    private static final Logger logger = LoggerFactory.getLogger(AppIdSupplier.class);

    public String get() {
        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey, TelemetryConfiguration.getActive());

        //it's possible the appId returned is null (e.g. async task is still pending or has failed). In this case, just
        //return and let the next request resolve the ikey.
        if (appId == null) {
            logger.debug("Application correlation Id could not be retrieved (e.g. task may be pending or failed)");
            return "";
        }
        return appId;
    }
}
