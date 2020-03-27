package com.microsoft.applicationinsights.internal.etw.events;

import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventId;

public class IpaInfo extends IpaEtwEventBase {

    public IpaInfo() {
        super();
    }

    public IpaInfo(IpaEtwEventBase event) {
        super(event);
    }

    @Override
    public IpaEtwEventId id() {
        return IpaEtwEventId.INFO;
    }
}