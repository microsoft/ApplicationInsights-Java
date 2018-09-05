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

package com.microsoft.applicationinsights.autoconfigure.helpers;

import org.apache.commons.lang3.StringUtils;

/**
 * A helper class to fetch instrumentation key from system properties or environment variables
 * @author Dhaval Doshi
 */
public class IkeyResolver {

  private static final String EXTERNAL_PROPERTY_IKEY_NAME = "APPLICATION_INSIGHTS_IKEY";
  private static final String EXTERNAL_PROPERTY_IKEY_NAME_SECONDARY = "APPINSIGHTS_INSTRUMENTATIONKEY";

    public static String getIkeyFromEnvironmentVariables() {
    String v = System.getProperty(EXTERNAL_PROPERTY_IKEY_NAME);
    if (StringUtils.isNotBlank(v)) {
      return v;
    }

    v = System.getProperty(EXTERNAL_PROPERTY_IKEY_NAME_SECONDARY);
    if (StringUtils.isNotBlank(v)) {
      return v;
    }

    // Second, try to find the i-key as an environment variable 'APPLICATION_INSIGHTS_IKEY' or 'APPINSIGHTS_INSTRUMENTATIONKEY'
    v = System.getenv(EXTERNAL_PROPERTY_IKEY_NAME);
    if (StringUtils.isNotBlank(v)) {
      return v;
    }
    v = System.getenv(EXTERNAL_PROPERTY_IKEY_NAME_SECONDARY);
    if (StringUtils.isNotBlank(v)) {
      return v;
    }

    return v;
  }
}
