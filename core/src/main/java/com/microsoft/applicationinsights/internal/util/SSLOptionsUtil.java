package com.microsoft.applicationinsights.internal.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import javax.annotation.Nullable;
import java.util.Arrays;

public class SSLOptionsUtil {

    public static final String APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY = "applicationinsights.ssl.protocols";

    @Nullable
    public static String[] getAllowedProtocols() {
        final String rawProp = System.getProperty(APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY);
        final String[] defaultValue = {"TLSv1.2"};
        if (rawProp == null) { // empty means defer to JVM defaults
            return defaultValue;
        }
        String[] customProtocols = Iterables.toArray(Splitter.on(',').trimResults().omitEmptyStrings().split(rawProp), String.class);
        if (customProtocols.length == 0) {
            InternalLogger.INSTANCE.info("Found application.ssl.protocols=''; using JVM default SSL protocols for HTTP client");
            return null;
        }

        if (InternalLogger.INSTANCE.isInfoEnabled()) {
            InternalLogger.INSTANCE.info("Found application.ssl.protocols='%s'; HTTP client will allow only these protocols", Arrays.toString(customProtocols));
        }
        return customProtocols;
    }

}
