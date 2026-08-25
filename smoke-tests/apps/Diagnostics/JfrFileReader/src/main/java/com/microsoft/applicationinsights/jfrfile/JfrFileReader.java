// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.jfrfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

public class JfrFileReader {

  public static boolean hasEventOfType(Path jfrFile, String event) throws IOException {
    return new RecordingFile(jfrFile)
        .readEventTypes()
        .stream().map(EventType::getName)
        .anyMatch(event::equals);
  }

  /**
   * Returns true if the recording contains at least one actual event instance of the given type.
   *
   * <p>Unlike {@link #hasEventOfType(Path, String)}, which only checks whether the event type is
   * registered in the recording metadata, this reads the recorded events and confirms one was
   * actually captured.
   */
  public static boolean hasEventInstanceOfType(Path jfrFile, String event) throws IOException {
    try (RecordingFile recordingFile = new RecordingFile(jfrFile)) {
      while (recordingFile.hasMoreEvents()) {
        RecordedEvent recordedEvent = recordingFile.readEvent();
        if (recordedEvent.getEventType().getName().equals(event)) {
          return true;
        }
      }
    }
    return false;
  }
}
