// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.jfrfile;

import java.io.File;
import java.io.IOException;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordingFile;

public class JfrFileReader {

  public static boolean hasEventOfType(File jfrFile, String event) throws IOException {
    return new RecordingFile(jfrFile.toPath())
        .readEventTypes()
        .stream().map(EventType::getName)
        .anyMatch(event::equals);
  }
}
