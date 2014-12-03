package com.microsoft.applicationinsights.datacontracts;

/**
 * This enumeration is used by ExceptionTelemetry to identify if and where exception was handled.
 */
public enum ExceptionHandledAt
{
    /**
     *  Exception was not handled. Application crashed.
     */
    Unhandled,

    /**
     *  Exception was handled in user code.
     */
    UserCode,

    /**
     *  Exception was handled by some platform handlers.
     */
    Platform
}
