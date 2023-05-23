// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.calibration;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ContextSwitchingRunner implements Iterable<Void> {

  private static final int NUMBER_OF_CALLS = 40000;

  private static final Object lock = new Object();

  private final List<Integer> threadCounts;

  public ContextSwitchingRunner() {
    threadCounts = Arrays.asList(100, 1000, 10, 100);
  }

  public int getRunCount() {
    return threadCounts.size();
  }

  public static void main(String[] args) {
    new ContextSwitchingRunner().runThreads(1000);
  }

  @SuppressWarnings("ThreadJoinLoop")
  private void runThreads(int threadCount) {
    Thread[] threads = new Thread[threadCount];

    for (int i = 0; i < threads.length; i++) {
      threads[i] =
          new Thread() {
            @SuppressWarnings("unused")
            private int value = 0;

            @Override
            public void run() {
              for (int i = 0; i < NUMBER_OF_CALLS; i++) {
                synchronized (lock) {
                  value++;
                }
              }
            }
          };
    }

    int numThreads = 0;
    // fork
    for (; numThreads < threads.length; numThreads++) {
      try {
        threads[numThreads].start();
      } catch (Error error) {
        // OS will not allow us to spawn enough threads, just use however many we have
        break;
      }
    }

    // join
    try {
      for (int i = 0; i < numThreads; i++) {
        if (threads[i] != null) {
          threads[i].join();
        }
      }
    } catch (InterruptedException e) {
      // Ignore
    }
  }

  @Override
  public Iterator<Void> iterator() {
    Iterator<Integer> countIterator = threadCounts.iterator();
    return new Iterator<Void>() {
      @Override
      public boolean hasNext() {
        return countIterator.hasNext();
      }

      @Override
      public Void next() {
        Integer threadCount = countIterator.next();
        runThreads(threadCount);
        return null;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Doesn't make sense to remove from this iterable");
      }
    };
  }
}
