// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import com.microsoft.applicationinsights.diagnostics.collection.libos.TwoStepUpdatable;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TwoStepProcReader implements TwoStepUpdatable, Closeable {

  private static final Logger logger = LoggerFactory.getLogger(TwoStepProcReader.class);

  protected RandomAccessFile file;
  protected String contents;

  TwoStepProcReader(String fileLocation) {
    this(new File(fileLocation));
  }

  public TwoStepProcReader(File candidate) {
    this(candidate, false);
  }

  public TwoStepProcReader(File candidate, boolean supressError) {
    super();
    try {
      file = new RandomAccessFile(candidate, "r");
    } catch (FileNotFoundException e) {
      if (!supressError) {
        logger.error("Failed to open proc net file", e);
      }
    }
  }

  protected abstract void parseLine(String line);

  @Override
  public void close() throws IOException {
    if (file != null) {
      file.close();
    }
  }

  @Override
  public void poll() {
    try {
      if (file != null) {
        contents = Proc.read(file);
      }
    } catch (IOException e) {
      logger.error("Failed to read stats for file", e);
    }
  }

  @Override
  public void update() {
    if (contents != null) {
      String[] lines = contents.split("\n");
      for (String line : lines) {
        if (trim()) {
          line = line.trim();
        }
        parseLine(line);
      }
    }
  }

  protected boolean trim() {
    return true;
  }
}
