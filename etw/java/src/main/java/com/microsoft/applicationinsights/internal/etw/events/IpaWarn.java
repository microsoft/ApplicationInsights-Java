package com.microsoft.applicationinsights.internal.etw.events;

import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventErrorBase;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventId;

public class IpaWarn extends IpaEtwEventErrorBase {

    public IpaWarn() {
        super();
    }

    public IpaWarn(IpaEtwEventBase event) {
        super(event);
    }

    @Override
    public IpaEtwEventId id() {
        return IpaEtwEventId.WARN;
    }

}