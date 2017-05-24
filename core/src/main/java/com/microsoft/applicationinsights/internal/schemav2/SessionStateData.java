/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.SessionState;

import com.google.common.base.Preconditions;

/**
 * Created by gupele on 2/19/2015.
 */
@Deprecated
public final class SessionStateData extends Domain {
    /**
     * Envelope Name for this telemetry.
     */
    private static final String SESSION_ENVELOPE_NAME = "Microsoft.ApplicationInsights.SessionState";

    /**
     * Base Type for this telemetry.
     */
    private static final String SESSION_BASE_TYPE = "Microsoft.ApplicationInsights.SessionStateData";

    private final int ver = 2;

    private SessionState state;

    public SessionStateData(SessionState state) {
        this.state = state;
    }

    public int getVer() {
        return ver;
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        writer.write("ver", ver);
        writer.write("state", state.toString());
    }

    @Override
    public String getEnvelopName() {
        return SESSION_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return SESSION_BASE_TYPE;
    }
}
