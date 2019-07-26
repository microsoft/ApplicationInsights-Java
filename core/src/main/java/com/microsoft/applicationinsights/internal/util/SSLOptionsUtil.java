package com.microsoft.applicationinsights.internal.util;

import com.google.common.base.Splitter;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SSLOptionsUtil {

    private SSLOptionsUtil() {}

    public static final String APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY = "applicationinsights.ssl.protocols";

    private static final String[] DEFAULT_PROTOCOLS;
    static {
        String[] proposed = new String[] {"TLSv1.3", "TLSv1.2"};
        DEFAULT_PROTOCOLS = filterSupportedProtocols(Arrays.asList(proposed), false);
        if (DEFAULT_PROTOCOLS.length == 0 && InternalLogger.INSTANCE.isErrorEnabled()) {
            InternalLogger.INSTANCE.error("Default protocols are not supported in this JVM: %s. System property '%s' can be used to configure supported SSL protocols.",
                    Arrays.toString(proposed), APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY);
        }
    }

    private static String[] filterSupportedProtocols(Iterable<String> defaultValue, boolean reportErrors) {
        List<String> supported = new ArrayList<>();
        for (String protocol : defaultValue) {
            try {
                SSLContext.getInstance(protocol);
                supported.add(protocol);
            } catch (NoSuchAlgorithmException e) {
                if (InternalLogger.INSTANCE.isErrorEnabled() && reportErrors) {
                    InternalLogger.INSTANCE.error("Could not find protocol '%s': %s", protocol, ExceptionUtils.getStackTrace(e));
                }
            }
        }
        return supported.toArray(new String[0]);
    }

    /**
     * <p>Finds the list of supported SSL/TLS protocols. If custom protocols are specified with the system property {@value #APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY}, this overrides the defaults.
     * An error will be logged if the property contains no supported protocols.</p>
     *
     * <p>If no supported protocols are specified, the defaults are used (see static constructor). If no default protocols are available on this JVM, an error is logged.</p>
     *
     * @return An array of supported protocols. If there are none found, an empty array.
     */
    public static String[] getAllowedProtocols() {
        final String rawProp = System.getProperty(APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY);
        if (rawProp == null) { // empty means defer to JVM defaults
            return DEFAULT_PROTOCOLS;
        }
        String[] customProtocols = filterSupportedProtocols(Splitter.on(',').trimResults().omitEmptyStrings().split(rawProp), true);
        if (customProtocols.length == 0) {
            if (InternalLogger.INSTANCE.isErrorEnabled()) {
                InternalLogger.INSTANCE.error("%s contained no supported protocols; using default: %s", APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY, Arrays.toString(DEFAULT_PROTOCOLS));
            }
            return DEFAULT_PROTOCOLS;
        }

        if (InternalLogger.INSTANCE.isInfoEnabled()) {
            InternalLogger.INSTANCE.info("Found %s='%s'; HTTP client will allow only these protocols", APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY, Arrays.toString(customProtocols));
        }
        return customProtocols;
    }

}
