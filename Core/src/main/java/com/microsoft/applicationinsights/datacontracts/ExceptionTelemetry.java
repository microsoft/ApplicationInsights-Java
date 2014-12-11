package com.microsoft.applicationinsights.datacontracts;

import com.microsoft.applicationinsights.extensibility.model.ExceptionData;
import com.microsoft.applicationinsights.extensibility.model.ExceptionDetails;
import com.microsoft.applicationinsights.extensibility.model.StackFrame;
import com.microsoft.applicationinsights.datacontracts.ExceptionHandledAt;
import com.microsoft.applicationinsights.util.MapUtil;
import com.microsoft.applicationinsights.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Telemetry used to track events.
 */
public class ExceptionTelemetry extends BaseTelemetry
{
    private final ExceptionData data;
    private Exception exception;

    private ExceptionTelemetry()
    {
        super();
        this.data = new ExceptionData();
        initialize(this.data.getProperties());
        this.setExceptionHandledAt(ExceptionHandledAt.Unhandled);
    }

    public ExceptionTelemetry(Exception exception)
    {
        this();
        this.setException(exception);

    }

    public Exception getException()
    {
        return exception;
    }

    public void setException(Exception exception)
    {
        this.exception = exception;
        updateException(exception);
    }

    public ExceptionHandledAt getExceptionHandledAt()
    {
        return Enum.valueOf(ExceptionHandledAt.class, this.data.getHandledAt());
    }

    public void setExceptionHandledAt(ExceptionHandledAt value)
    {
        this.data.setHandledAt(value.toString());
    }

    public Map<String,Double> getMetrics()
    {
        return this.data.getMeasurements();
    }

    public List<ExceptionDetails> getExceptions()
    {
        return this.data.getExceptions();
    }



    @Override
    public void sanitize()
    {
        MapUtil.sanitizeProperties(this.getProperties());
        MapUtil.sanitizeMeasurements(this.getMetrics());
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException
    {
        writer.writeStartObject();

        writer.writeProperty("ver", 1);
        writer.writeProperty("name", "Microsoft.ApplicationInsights.Exception");
        writer.writeProperty("time", this.getTimestamp());

        getContext().serialize(writer);

        writer.writePropertyName("data");

        {
            writer.writeStartObject();

            writer.writeProperty("type", "Microsoft.ApplicationInsights.ExceptionData");

            writer.writePropertyName("item");
            {
                writer.writeStartObject();

                writer.writeProperty("ver", this.data.getVer());
                writer.writeProperty("handledAt",
                        StringUtil.populateRequiredStringWithNullValue(this.data.getHandledAt(), "handledAt", ExceptionTelemetry.class.getName()));
                writer.writeMetricsProperty("measurements", this.data.getMeasurements());
                writer.writeProperty("properties", this.data.getProperties());

                writer.writePropertyName("exceptions");
                {
                    writer.writeStartArray();

                    serialize(this.getExceptions(), writer);

                    writer.writeEndArray();
                }

                writer.writeEndObject();
            }

            writer.writeEndObject();
        }

        writer.writeEndObject();
    }

    private void serialize(List<ExceptionDetails> exceptions, JsonWriter writer) throws IOException
    {
        int index = 0;

        for (ExceptionDetails exceptionDetails : exceptions)
        {
            if (index++ != 0)
                writer.writeComma();

            writer.writeStartObject();

            writer.writeProperty("id", exceptionDetails.getId());
            if (exceptionDetails.getOuterId() != 0)
                writer.writeProperty("outerId", exceptionDetails.getOuterId());

            writer.writeProperty("typeName",
                    StringUtil.populateRequiredStringWithNullValue(exceptionDetails.getTypeName(), "typeName", ExceptionTelemetry.class.getName()));
            writer.writeProperty("message",
                    StringUtil.populateRequiredStringWithNullValue(exceptionDetails.getMessage(), "message", ExceptionTelemetry.class.getName()));

            if (exceptionDetails.getHasFullStack())
                writer.writeProperty("hasFullStack", exceptionDetails.getHasFullStack());

            writer.writeProperty("stack", exceptionDetails.getStack());

            if (exceptionDetails.getParsedStack().size() > 0)
            {
                writer.writePropertyName("parsedStack");
                {
                    writer.writeStartArray();

                    int frameIdx = 0;

                    for (StackFrame frame: exceptionDetails.getParsedStack())
                    {
                        if (frameIdx++ != 0)
                            writer.writeComma();

                        writer.writeStartObject();

                        serialize(frame, writer);

                        writer.writeEndObject();
                    }

                    writer.writeEndArray();
                }
            }

            writer.writeEndObject();
        }
    }

    private void serialize(StackFrame frame, JsonWriter writer) throws IOException
    {
        writer.writeProperty("level", frame.getLevel());
        writer.writeProperty(
                "method",
                StringUtil.populateRequiredStringWithNullValue(frame.getMethod(), "StackFrameMethod", ExceptionTelemetry.class.getName()));
        writer.writeProperty("fileName", frame.getFileName());

        // 0 means it is unavailable
        if (frame.getLine() != 0)
        {
            writer.writeProperty("line", frame.getLine());
        }
    }

    private void updateException(Exception exception)
    {
        ArrayList<ExceptionDetails> exceptions = new ArrayList<ExceptionDetails>();
        convertExceptionTree(exception, null, exceptions);

        this.data.setExceptions(exceptions);
    }

    private static void convertExceptionTree(Throwable exception, ExceptionDetails parentExceptionDetails, List<ExceptionDetails> exceptions)
    {
        if (exception == null)
            exception = new Exception("");

        ExceptionDetails exceptionDetails = createWithStackInfo(exception, parentExceptionDetails);
        exceptions.add(exceptionDetails);

        if (exception.getCause() != null)
        {
            convertExceptionTree(exception.getCause(), exceptionDetails, exceptions);
        }
    }

    private static ExceptionDetails createWithStackInfo(Throwable exception, ExceptionDetails parentExceptionDetails)
    {
        if (exception == null)
        {
            throw new IllegalArgumentException("exception cannot be null");
        }

        ExceptionDetails exceptionDetails = new ExceptionDetails();
        exceptionDetails.setId(StringUtil.generateRandomIntegerId());
        exceptionDetails.setTypeName(exception.getClass().getName());
        exceptionDetails.setMessage(exception.getMessage());

        if (parentExceptionDetails != null)
            exceptionDetails.setOuterId(parentExceptionDetails.getId());

        StackTraceElement[] trace = exception.getStackTrace();

        if (trace != null && trace.length > 0)
        {
            List<StackFrame> stack = exceptionDetails.getParsedStack();

            // We need to present the stack trace in reverse order.

            for (int idx = 0; idx < trace.length; idx++)
            {
                StackTraceElement elem = trace[idx];

                if (elem.isNativeMethod()) continue;

                String className = elem.getClassName();

                StackFrame frame = new StackFrame();
                frame.setLevel(idx);
                frame.setFileName(elem.getFileName());
                frame.setLine(elem.getLineNumber());

                if (!StringUtil.isNullOrEmpty(className))
                    frame.setMethod(elem.getClassName() + "." + elem.getMethodName());
                else
                    frame.setMethod(elem.getMethodName());
                stack.add(frame);
            }

            exceptionDetails.setHasFullStack(true); // TODO: sanitize and trim exception stack trace.
        }

        return exceptionDetails;
    }
}
