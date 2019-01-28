package com.microsoft.applicationinsights.telemetry;

public class ExceptionTelemetryOptions {
    private int maxStackSize;
    private int maxExceptionTraceLength;

    public ExceptionTelemetryOptions(Integer maxStackSize, Integer maxExceptionTraceLength) {
        this.maxStackSize = maxStackSize != null ? maxStackSize : Integer.MAX_VALUE;
        this.maxExceptionTraceLength = maxExceptionTraceLength != null ? maxExceptionTraceLength : Integer.MAX_VALUE;
    }

    public static ExceptionTelemetryOptions empty(){
        return new ExceptionTelemetryOptions(null, null);
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public int getMaxExceptionTraceLength() {
        return maxExceptionTraceLength;
    }
}
