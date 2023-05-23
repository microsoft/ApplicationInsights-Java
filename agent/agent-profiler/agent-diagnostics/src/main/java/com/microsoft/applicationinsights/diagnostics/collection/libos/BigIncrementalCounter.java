// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos;

import java.math.BigInteger;
import javax.annotation.Nullable;

/** {@link IncrementalCounter} but using a BigInteger. */
public class BigIncrementalCounter {
  public static final BigInteger NO_PREVIOUS_INCREMENT_VALUE = BigInteger.valueOf(-1);
  @Nullable private BigInteger lastSeenValue;
  @Nullable private BigInteger increment;

  public BigIncrementalCounter() {
    lastSeenValue = null;
    increment = null;
  }

  public void newValue(BigInteger newValue) {
    if (lastSeenValue != null) {
      increment = newValue.subtract(lastSeenValue);
    }
    lastSeenValue = newValue;
  }

  public void newValue(String newValue) {
    try {
      newValue(new BigInteger(newValue));
    } catch (NumberFormatException e) {
      // Nop
    }
  }

  public void newValue(long newValue) {
    newValue(BigInteger.valueOf(newValue));
  }

  @Nullable
  public BigInteger getIncrement() {
    return increment;
  }

  public BigInteger getValue() {
    return lastSeenValue;
  }

  public BigInteger getNonNullIncrement() {
    if (increment == null) {
      return NO_PREVIOUS_INCREMENT_VALUE;
    }
    return increment;
  }
}
