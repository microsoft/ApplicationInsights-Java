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

package com.microsoft.gcmonitor.collectors;

class IncrementalCounter {

  private static final long NO_PREVIOUS = -1;

  private long lastSeenValue;
  private long increment;

  public IncrementalCounter() {
    reset();
  }

  public boolean hasPrevious() {
    return increment != NO_PREVIOUS;
  }

  public void newValue(long newValue) {
    if (lastSeenValue != NO_PREVIOUS) {
      if (newValue > 0) {
        increment = newValue - lastSeenValue;
      } else {
        increment = (Long.MAX_VALUE - lastSeenValue) + (newValue - Long.MIN_VALUE);
      }
      if (increment < 0) {
        increment = 0;
      }
    }
    lastSeenValue = newValue;
  }

  public long getIncrement() {
    return increment;
  }

  public long getValue() {
    return lastSeenValue;
  }

  @Override
  public String toString() {
    return "IncrementalCounter [lastSeenValue=" + lastSeenValue + ", increment=" + increment + "]";
  }

  public void reset() {
    lastSeenValue = NO_PREVIOUS;
    increment = NO_PREVIOUS;
  }
}
