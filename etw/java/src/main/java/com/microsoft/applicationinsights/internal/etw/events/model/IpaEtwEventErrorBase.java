package com.microsoft.applicationinsights.internal.etw.events.model;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;

public abstract class IpaEtwEventErrorBase extends IpaEtwEventBase {
    private IThrowableProxy stacktrace;

    public IpaEtwEventErrorBase() {
        super();
    }
    public IpaEtwEventErrorBase(IpaEtwEventBase evt) {
        super(evt);
    }

    /**
     * @return the stacktrace
     */
    public String getStacktraceString() {
        // if stacktrace == null, returns ""
        return ThrowableProxyUtil.asString(stacktrace);
    }

    /**
     * @param stacktrace the stacktrace to set
     */
    public void setStacktrace(IThrowableProxy stacktrace) {
        this.stacktrace = stacktrace;
    }
}