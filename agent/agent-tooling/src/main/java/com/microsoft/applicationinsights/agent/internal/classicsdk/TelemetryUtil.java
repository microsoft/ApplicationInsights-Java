// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.ExceptionDetailBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.StackFrameBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TelemetryUtil {

  private static final Logger logger = LoggerFactory.getLogger(TelemetryUtil.class);

  // Breeze will reject parsedStack exceeding 65536 bytes. Each char is 2 bytes long.
  private static final int MAX_PARSED_STACK_LENGTH = 32768;

  static List<ExceptionDetailBuilder> getExceptions(Throwable throwable) {
    List<ExceptionDetailBuilder> exceptions = new ArrayList<>();
    convertExceptionTree(throwable, null, exceptions, Integer.MAX_VALUE);
    return exceptions;
  }

  private static void convertExceptionTree(
      Throwable exception,
      @Nullable ExceptionDetailBuilder parentExceptionDetails,
      List<ExceptionDetailBuilder> exceptions,
      int stackSize) {
    if (exception == null) {
      exception = new Exception("");
    }

    if (stackSize == 0) {
      return;
    }

    ExceptionDetailBuilder exceptionDetails =
        createWithStackInfo(exception, parentExceptionDetails);
    exceptions.add(exceptionDetails);

    if (exception.getCause() != null) {
      convertExceptionTree(exception.getCause(), exceptionDetails, exceptions, stackSize - 1);
    }
  }

  private static ExceptionDetailBuilder createWithStackInfo(
      Throwable exception, @Nullable ExceptionDetailBuilder parentExceptionDetails) {
    if (exception == null) {
      throw new IllegalArgumentException("exception cannot be null");
    }

    ExceptionDetailBuilder exceptionDetails = new ExceptionDetailBuilder();
    exceptionDetails.setId(exception.hashCode());
    exceptionDetails.setTypeName(exception.getClass().getName());

    String exceptionMessage = exception.getMessage();
    if (Strings.isNullOrEmpty(exceptionMessage)) {
      exceptionMessage = exception.getClass().getName();
    }
    exceptionDetails.setMessage(exceptionMessage);

    if (parentExceptionDetails != null) {
      exceptionDetails.setOuter(parentExceptionDetails);
    }

    StackTraceElement[] trace = exception.getStackTrace();
    exceptionDetails.setHasFullStack(true);

    if (trace != null && trace.length > 0) {
      List<StackFrameBuilder> stack = new ArrayList<>();

      // We need to present the stack trace in reverse order.
      int stackLength = 0;
      for (int idx = 0; idx < trace.length; idx++) {
        StackTraceElement elem = trace[idx];

        if (elem.isNativeMethod()) {
          continue;
        }

        String className = elem.getClassName();

        StackFrameBuilder frame = new StackFrameBuilder();
        frame.setLevel(idx);
        frame.setFileName(elem.getFileName());
        frame.setLine(elem.getLineNumber());

        String method;
        if (!Strings.isNullOrEmpty(className)) {
          method = elem.getClassName() + "." + elem.getMethodName();
        } else {
          method = elem.getMethodName();
        }
        frame.setMethod(method);

        stackLength += getStackFrameLength(method, elem.getFileName(), null);
        if (stackLength > MAX_PARSED_STACK_LENGTH) {
          exceptionDetails.setHasFullStack(false);
          logger.debug(
              "parsedStack is exceeding 65536 bytes capacity. It is truncated from full {} frames to partial {} frames.",
              trace.length,
              stack.size());
          break;
        }

        stack.add(frame);
      }

      exceptionDetails.setParsedStack(stack);
    }

    return exceptionDetails;
  }

  /** Returns the stack frame length for only the strings in the stack frame. */
  // this is the same logic used to limit length on the Breeze side
  private static int getStackFrameLength(
      String method, String fileName, @Nullable String assembly) {
    return getStackFrameLength(method)
        + getStackFrameLength(fileName)
        + getStackFrameLength(assembly);
  }

  private static int getStackFrameLength(@Nullable String text) {
    return text == null ? 0 : text.length();
  }

  private TelemetryUtil() {}
}
