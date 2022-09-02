// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitortests;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Generates a series of GC events. Should be run with roughly 50mb of heap (depends on collector)
 */
public class GcEventGenerator {

  volatile List<MemoryConsumer> memory = new ArrayList<>();
  volatile MemoryConsumer singleMemory;

  // Do 1k chunks
  private static final int CHUNK_SIZE = 1024;

  // Run with 50mb of memory
  public static void main(String[] args) throws InterruptedException {
    new GcEventGenerator().run();
  }

  @SuppressWarnings("SystemOut")
  private void run() throws InterruptedException {
    System.out.println("Hit return to start");

    // Block until consumer sends the ready signal
    Scanner scanner = new Scanner(System.in, UTF_8.name());
    System.out.println(scanner.nextLine());

    // Allocate 1mb
    for (int i = 0; i < 1024; i++) {
      memory.add(new MemoryConsumer(CHUNK_SIZE));
    }

    // Create 100mb of transient memory, invoking a number of YG gc's and promoting some to OG
    for (int i = 0; i < 100 * 1024; i++) {
      singleMemory = new MemoryConsumer(CHUNK_SIZE);
    }

    // Run full GC
    System.gc();

    // Allocate 30mb, 25% of which is transient
    for (int i = 0; i < 30 * 1024; i++) {
      if ((i % 4) == 0) {
        singleMemory = new MemoryConsumer(CHUNK_SIZE);
      } else {
        memory.add(new MemoryConsumer(CHUNK_SIZE));
      }
    } // Heap at 23.5mb

    // Create 100mb of transient memory, invoking a number of YG gc's and promoting 40mb to OG
    for (int i = 0; i < 100 * 1024; i++) {
      singleMemory = new MemoryConsumer(CHUNK_SIZE);
    }

    // free up 10mb
    for (int i = 0; i < 10 * 1024; i++) {
      memory.remove(0);
    } // Heap at 13.5mb

    // Allocate 43mb, 50% of which is transient
    for (int i = 0; i < 43 * 1024; i++) {
      if ((i % 2) == 0) {
        singleMemory = new MemoryConsumer(CHUNK_SIZE);
      } else {
        memory.add(new MemoryConsumer(CHUNK_SIZE));
      }
    } // Heap at 35mb

    // free up everything
    memory.clear();

    // Run full GC
    System.gc();
    // free up everything
    memory.clear();
    // Run full GC
    System.gc();

    // Seems if the JVM exits too quickly MX beans do not report, give it some time

    System.out.println("Hit return to exit");
    System.out.println(scanner.nextLine());
  }

  static class MemoryConsumer {
    private final byte[] memory;

    MemoryConsumer(int sizeInBytes) {
      this.memory = new byte[sizeInBytes];
    }

    public byte[] getMemory() {
      return memory;
    }
  }
}
