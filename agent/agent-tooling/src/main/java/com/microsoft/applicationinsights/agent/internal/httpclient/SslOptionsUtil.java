/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.httpclient;

import com.microsoft.applicationinsights.agent.internal.common.Strings;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslOptionsUtil {

  private static final Logger logger = LoggerFactory.getLogger(SslOptionsUtil.class);

  private SslOptionsUtil() {}

  public static final String APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY =
      "applicationinsights.ssl.protocols";

  private static final String[] DEFAULT_SUPPORTED_PROTOCOLS;
  private static final String[] DEFAULT_PROTOCOLS = new String[] {"TLSv1.3", "TLSv1.2"};

  static {
    DEFAULT_SUPPORTED_PROTOCOLS = filterSupportedProtocols(DEFAULT_PROTOCOLS, false);
    if (DEFAULT_SUPPORTED_PROTOCOLS.length == 0 && logger.isErrorEnabled()) {
      logger.error(
          "Default protocols are not supported in this JVM: {}. System property '{}' can be used to configure supported SSL protocols.",
          Arrays.toString(DEFAULT_PROTOCOLS),
          APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY);
    }
  }

  private static String[] filterSupportedProtocols(String[] defaultValue, boolean reportErrors) {
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
   * Finds the list of supported SSL/TLS protocols. If custom protocols are specified with the
   * system property {@value #APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY}, this overrides the
   * defaults. An error will be logged if the property contains no supported protocols.
   *
   * <p>If no supported protocols are specified, the defaults are used (see static constructor). If
   * no default protocols are available on this JVM, an error is logged.
   *
   * @return An array of supported protocols. If there are none found, an empty array.
   * @throws NoSupportedProtocolsException If the defaults are to be used and none of the defaults
   *     are supported by this JVM
   */
  // FIXME (trask) do we need to hook this into new Azure Http Client?
  public static String[] getAllowedProtocols() {
    String rawProp = System.getProperty(APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY);
    if (rawProp == null) {
      return defaultSupportedProtocols();
    }

    if (Strings.isNullOrEmpty(rawProp)) {
      if (logger.isWarnEnabled()) {
        logger.warn(
            "{} specifies no protocols; using defaults: {}",
            APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY,
            Arrays.toString(DEFAULT_SUPPORTED_PROTOCOLS));
      }
      return defaultSupportedProtocols();
    }

    String[] customProtocols = filterSupportedProtocols(rawProp.split(","), true);
    if (customProtocols.length == 0) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "{} contained no supported protocols: '{}'; using default: {}",
            APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY,
            rawProp,
            Arrays.toString(DEFAULT_SUPPORTED_PROTOCOLS));
      }
      return defaultSupportedProtocols();
    }

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Found {}='{}'; HTTP client will allow only these protocols",
          APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY,
          Arrays.toString(customProtocols));
    }
    return customProtocols;
  }

  private static String[] defaultSupportedProtocols() {
    if (DEFAULT_SUPPORTED_PROTOCOLS.length == 0) {
      throw new NoSupportedProtocolsException(
          String.format(
              "None of the default TLS protocols are supported by this JVM: %s. Use the system property '%s' to override.",
              Arrays.toString(DEFAULT_PROTOCOLS), APPLICATION_INSIGHTS_SSL_PROTOCOLS_PROPERTY));
    }
    return DEFAULT_SUPPORTED_PROTOCOLS;
  }
}
