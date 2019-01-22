package com.microsoft.applicationinsights.telemetry;

public class ExceptionTelemetryOptions {
    private int maxStackSize;
    private int maxTraceLength;

    public ExceptionTelemetryOptions(Integer maxStackSize, Integer maxTraceLength) {
        this.maxStackSize = maxStackSize != null ? maxStackSize : Integer.MAX_VALUE;
        this.maxTraceLength = maxTraceLength != null ? maxTraceLength : Integer.MAX_VALUE;
    }

    public static ExceptionTelemetryOptions empty(){
        return new ExceptionTelemetryOptions(null, null);
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public int getMaxTraceLength() {
        return maxTraceLength;
    }
}
