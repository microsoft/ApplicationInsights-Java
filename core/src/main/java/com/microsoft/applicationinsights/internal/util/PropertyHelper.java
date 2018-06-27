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

package com.microsoft.applicationinsights.internal.util;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.lang3.exception.ExceptionUtils;

/** Helper class for reading data from a properties file found on the class path. */
public final class PropertyHelper {
  public static final String SDK_VERSION_FILE_NAME = "sdk-version.properties";
  static final String STARTER_VERSION_FILE_NAME = "starter-version.properties";

  private PropertyHelper() {}

  /**
   * Reads the properties from a properties file.
   *
   * @param name of the properties file.
   * @return A {@link Properties} object containing the properties read from the provided file.
   * @throws IOException in case
   */
  public static Properties getProperties(String name) throws IOException {
    Properties props = new Properties();
    ClassLoader classLoader = PropertyHelper.class.getClassLoader();

    // Look in the class loader's default location.
    InputStream inputStream = classLoader.getResourceAsStream(name);
    if (inputStream != null) {
      try {
        props.load(inputStream);
      } finally {
        inputStream.close();
      }
    }

    return props;
  }

  /**
   * A method that loads the properties file that contains SDK-Version data.
   *
   * @return The properties or null if not found.
   */
  public static Properties getSdkVersionProperties() {
    try {
      return getProperties(SDK_VERSION_FILE_NAME);
    } catch (IOException e) {
      InternalLogger.INSTANCE.error("Could not find sdk version file '%s'", SDK_VERSION_FILE_NAME);
      InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
    }

    return null;
  }

  /**
   * A method that loads the properties file that contains the AI SpringBootStarter version number
   *
   * @return The properties or null if not found.
   */
  public static Properties getStarterVersionProperties() {
    try {
      return getProperties(STARTER_VERSION_FILE_NAME);
    } catch (IOException e) {
      InternalLogger.INSTANCE.trace(
          "Could not find starter version file: %s," + "stack trace is: ",
          SDK_VERSION_FILE_NAME, ExceptionUtils.getStackTrace(e));
    }
    return null;
  }
}
