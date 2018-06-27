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

package com.microsoft.applicationinsights.core.volume;

import java.util.concurrent.atomic.AtomicInteger;

/** Created by gupele on 2/5/2015. */
final class TestResultsVerifier {
  private Thread waitingThread;
  private boolean done;
  private TestStatus status;
  private int numberOfExpected;
  private long elapsed;

  private AtomicInteger arrivedEvents = new AtomicInteger(0);

  public TestResultsVerifier() {}

  private static long secondsToMillis(long seconds) {
    return seconds * 1000;
  }

  public void reset(int numberOfExpected) {
    arrivedEvents.set(0);
    this.numberOfExpected = numberOfExpected;
    status = null;
    done = false;
    elapsed = System.nanoTime();
  }

  public void notifyEventsArrival(int number) {
    int currentArrived = arrivedEvents.addAndGet(number);
    if (currentArrived == numberOfExpected) {
      setSucceeded();
    }
  }

  public TestStats getResults(long sendTimeInNanos, int acceptedUntilEndOfSending) {
    return new TestStats(
        numberOfExpected,
        arrivedEvents.get(),
        status,
        elapsed,
        sendTimeInNanos,
        acceptedUntilEndOfSending);
  }

  public void waitFor(long maxTimeToWaitInSeconds) {
    try {
      this.waitingThread = Thread.currentThread();
      Thread.sleep(secondsToMillis(maxTimeToWaitInSeconds));
      onWaitExpired();
    } catch (InterruptedException e) {
    } finally {
    }
  }

  public int getCurrentAccepted() {
    return arrivedEvents.get();
  }

  private synchronized void onWaitExpired() {
    if (done) {
      return;
    }

    int numberArrived = arrivedEvents.get();
    if (numberOfExpected > numberArrived) {
      setResult(TestStatus.EXPIRED);
    } else {
      setResult(TestStatus.FAILED);
    }
  }

  private synchronized void setSucceeded() {
    if (done) {
      return;
    }

    setResult(TestStatus.SUCCEEDED);
    signalTestIsDone();
  }

  private void setResult(TestStatus status) {
    elapsed = System.nanoTime() - elapsed;

    this.status = status;
    done = true;
  }

  private void signalTestIsDone() {
    if (waitingThread != null) {
      waitingThread.interrupt();
    }
  }
}
