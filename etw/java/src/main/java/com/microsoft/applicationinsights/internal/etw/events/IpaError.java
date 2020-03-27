package com.microsoft.applicationinsights.internal.etw.events;

import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventErrorBase;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventId;

/**
 * JavaIpaError
 */
public class IpaError extends IpaEtwEventErrorBase {

    public IpaError() {
        super();
    }

    public IpaError(IpaEtwEventBase event) {
        super(event);
    }

    @Override
    public IpaEtwEventId id() {
        return IpaEtwEventId.ERROR;
    }

}