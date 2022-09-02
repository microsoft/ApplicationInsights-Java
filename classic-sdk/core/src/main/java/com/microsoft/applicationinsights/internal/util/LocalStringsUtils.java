// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

public class LocalStringsUtils {

  private static final SimpleDateFormat simpleDateFormat =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

  public static boolean isNullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }

  @SuppressFBWarnings(
      value = "SECPR", // Predictable pseudorandom number generator
      justification = "Predictable random is ok for telemetry id")
  public static String generateRandomIntegerId() {
    // avoid using Math.abs(rand.nextLong()) because Math.abs(Long.MIN_VALUE) is negative
    long rand = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    return String.valueOf(rand);
  }

  public static String formatDate(Date value) {
    synchronized (simpleDateFormat) {
      return simpleDateFormat.format(value);
    }
  }

  private LocalStringsUtils() {}
}
