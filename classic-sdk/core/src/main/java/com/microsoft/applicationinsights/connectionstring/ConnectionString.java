// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.connectionstring;

public final class ConnectionString {

  /**
   * When using this method to initialize the connection string after startup, you will need to set
   * {@code startWithoutConnectionString} to {@code true} inside of the {@code
   * applicationinsights.json} configuration file, e.g.
   *
   * <pre>
   *   {
   *     "startWithoutConnectionString": true
   *   }
   * </pre>
   *
   * If the connection string has already been set then this method logs a warning and does nothing.
   */
  public static void init(String connectionString) {
    // this methods is implemented by the Application Insights Java agent
  }

  private ConnectionString() {}
}
