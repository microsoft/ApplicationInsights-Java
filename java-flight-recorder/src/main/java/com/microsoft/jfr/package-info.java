// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
/**
 * This package provides API for controlling Java Flight Recordings (JFR) through Java Management Extensions (JMX).
 *
 * JDK 9 introduced the {@code jdk.jfr} API which is not available in JDK 8. The
 * {@code jdk.management.jfr.FlightRecorderMXBean} is available in Java 8 and higher.
 * By relying on JMX and the {@code jdk.management.jfr.FlightRecorderMXBean},
 * the {@code com.microsoft.jfr} package provides access to JFR on local or remote JVMs.
 */
package com.microsoft.jfr;
