package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventId;

public class IpaVerbose extends IpaEtwEventBase {

    public IpaVerbose() {
        super();
    }

    public IpaVerbose(IpaEtwEventBase event) {
        super(event);
    }

    @Override
    public IpaEtwEventId id() {
        return IpaEtwEventId.VERBOSE;
    }
}