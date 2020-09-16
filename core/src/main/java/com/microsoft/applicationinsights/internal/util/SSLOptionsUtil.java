package com.microsoft.applicationinsights.internal.util;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SSLOptionsUtil {

    private static final Logger logger = LoggerFactory.getLogger(SSLOptionsUtil.class);

    private SSLOptionsUtil() {}

    public static final String APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY = "applicationinsights.ssl.protocols";

    private static final String[] DEFAULT_SUPPORTED_PROTOCOLS;
    private static final String[] DEFAULT_PROTOCOLS = new String[] {"TLSv1.3", "TLSv1.2"};

    static {
        DEFAULT_SUPPORTED_PROTOCOLS = filterSupportedProtocols(Arrays.asList(DEFAULT_PROTOCOLS), false);
        if (DEFAULT_SUPPORTED_PROTOCOLS.length == 0 && logger.isErrorEnabled()) {
            logger.error("Default protocols are not supported in this JVM: {}. System property '{}' can be used to configure supported SSL protocols.",
                    Arrays.toString(DEFAULT_PROTOCOLS), APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY);
        }
    }

    private static String[] filterSupportedProtocols(Iterable<String> defaultValue, boolean reportErrors) {
        List<String> supported = new ArrayList<>();
        for (String protocol : defaultValue) {
            try {
                SSLContext.getInstance(protocol);
                supported.add(protocol);
            } catch (NoSuchAlgorithmException e) {
                if (logger.isErrorEnabled() && reportErrors) {
                    logger.error("Could not find protocol '{}'", protocol, e);
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
     * @throws NoSupportedProtocolsException If the defaults are to be used and none of the defaults are supported by this JVM
     */
    public static String[] getAllowedProtocols() {
        final String rawProp = System.getProperty(APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY);
        if (rawProp == null) {
            return defaultSupportedProtocols();
        }

        if (Strings.isNullOrEmpty(rawProp)) {
            if (logger.isWarnEnabled()) {
                logger.warn("{} specifies no protocols; using defaults: {}", APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY, Arrays.toString(DEFAULT_SUPPORTED_PROTOCOLS));
            }
            return defaultSupportedProtocols();
        }

        String[] customProtocols = filterSupportedProtocols(Splitter.on(',').trimResults().omitEmptyStrings().split(rawProp), true);
        if (customProtocols.length == 0) {
            if (logger.isErrorEnabled()) {
                logger.error("{} contained no supported protocols: '{}'; using default: {}", APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY, rawProp, Arrays.toString(DEFAULT_SUPPORTED_PROTOCOLS));
            }
            return defaultSupportedProtocols();
        }

        if (logger.isInfoEnabled()) {
            logger.info("Found {}='{}'; HTTP client will allow only these protocols", APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY, Arrays.toString(customProtocols));
        }
        return customProtocols;
    }

    private static String[] defaultSupportedProtocols() {
        if (DEFAULT_SUPPORTED_PROTOCOLS.length == 0) {
            throw new NoSupportedProtocolsException(String.format("None of the default TLS protocols are supported by this JVM: %s. Use the system property '%s' to override.", Arrays.toString(DEFAULT_PROTOCOLS), APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY));
        }
        return DEFAULT_SUPPORTED_PROTOCOLS;
    }

}
