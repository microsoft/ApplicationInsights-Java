package com.microsoft.applicationinsights.internal.etw.events;

import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventErrorBase;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventId;

/**
 * JavaIpaCritical
 */
public class IpaCritical extends IpaEtwEventErrorBase {

    public IpaCritical() {
        super();
    }

    public IpaCritical(IpaEtwEventBase event) {
        super(event);
    }

    @Override
    public IpaEtwEventId id() {
        return IpaEtwEventId.CRITICAL;
    }
}