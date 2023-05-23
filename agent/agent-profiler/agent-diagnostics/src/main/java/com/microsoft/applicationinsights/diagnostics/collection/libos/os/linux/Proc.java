// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/** Util for reading data from proc files */
final class Proc {

  static final File TOP_DIR = new File("/proc/");

  private static final int BUFFER_SIZE = 1024 * 3;
  private static final byte[] buffer = new byte[BUFFER_SIZE];

  private Proc() {}

  static String read(File file) throws IOException {
    try (RandomAccessFile resource = new RandomAccessFile(file, "r")) {
      return read(resource);
    }
  }

  static String read(RandomAccessFile resource) throws IOException {
    resource.seek(0);
    int totalRead = resource.read(buffer, 0, buffer.length);
    if (totalRead == -1) {
      return "";
    }
    return new String(buffer, 0, totalRead, StandardCharsets.UTF_8);
  }
}
