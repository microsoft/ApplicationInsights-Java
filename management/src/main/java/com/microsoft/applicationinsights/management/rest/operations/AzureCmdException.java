package com.microsoft.applicationinsights.management.rest.operations;

/**
 * Created by yonisha on 4/19/2015.
 */
import java.io.PrintWriter;
import java.io.StringWriter;

public class AzureCmdException extends Exception {
    private String mErrorLog;

    public AzureCmdException(String message, String errorLog) {
        super(message);

        mErrorLog = errorLog;
    }

    public AzureCmdException(String message, Throwable throwable) {
        super(message, throwable);

        if (throwable instanceof AzureCmdException) {
            mErrorLog = ((AzureCmdException) throwable).getErrorLog();
        } else {
            StringWriter sw = new StringWriter();
            PrintWriter writer = new PrintWriter(sw);

            throwable.printStackTrace(writer);
            writer.flush();

            mErrorLog = sw.toString();
        }
    }

    public String getErrorLog() {
        return mErrorLog;
    }
}