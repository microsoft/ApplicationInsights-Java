// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;

class CommandExecutor {

  private CommandExecutor() {}

  static String execute(String[] command) {
    IllegalStateException exitException = null;
    String result = null;
    try {
      Process process = new ProcessBuilder(command).start();
      int exitValue = process.waitFor();
      exitException = buildExitValueExceptionIfNecessary(command, exitValue, process);
      if (exitException == null) {
        InputStream inputStream = process.getInputStream();
        result = toString(inputStream);
      }
      process.destroy();
    } catch (IllegalStateException | IOException | InterruptedException e) {
      throw combineExceptionsIfNecessary(exitException, e, command);
    }
    if (exitException != null) {
      throw exitException;
    }
    return result;
  }

  static String executeWithoutException(String[] command, Logger startupLogger) {
    try {
      return execute(command);
    } catch (RuntimeException e) {
      startupLogger.error("Error when executing command " + Arrays.asList(command) + ".", e);
      if (e.getSuppressed().length == 1) {
        return e.getMessage() + " (Suppressed: " + e.getSuppressed()[0] + ")";
      }
      return e.getMessage();
    }
  }

  @Nullable
  private static IllegalStateException buildExitValueExceptionIfNecessary(
      String[] command, int exitValue, Process directivesClearProcess) throws IOException {
    if (exitValue != 0) {
      InputStream errorStream = directivesClearProcess.getErrorStream();
      String error = toString(errorStream);
      return new IllegalStateException(
          "Error executing command " + Arrays.asList(command) + ": " + error + ".");
    }
    return null;
  }

  private static String toString(InputStream inputStream) throws IOException {
    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()))) {
      return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
    }
  }

  private static IllegalStateException combineExceptionsIfNecessary(
      IllegalStateException exitValueException, Exception e, String[] command) {
    IllegalStateException exceptionWithMessage =
        new IllegalStateException(
            "Error related to the execution of " + Arrays.asList(command) + ".", e);
    if (exitValueException == null) {
      return exceptionWithMessage;
    }
    exitValueException.addSuppressed(exceptionWithMessage);
    return exitValueException;
  }
}
