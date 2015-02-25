/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.telemetry;

import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.applicationinsights.internal.schemav2.SessionStateData;

/**
 * Telemetry type used to track user sessions.
 */
public final class SessionStateTelemetry extends BaseTelemetry<SessionStateData> {
    private final SessionStateData data;

    /**
     * Default initialization of a new instance of the class.
     * The session state will be set to 'Start'
     */
    public SessionStateTelemetry() {
        this(SessionState.Start);
    }

    /**
     * Initializes a new instance of the class with the specified <code>sessionState</code>
     * @param sessionState value indicating state of the user session.
     */
    public SessionStateTelemetry(SessionState sessionState) {
        super();
        data = new SessionStateData(sessionState);
        initialize(new ConcurrentHashMap<String, String>());
    }

    /**
     * Gets the session state.
     * @return session state.
     */
    public SessionState getSessionState() {
        return data.getState();
    }

    /**
     * Sets the session state.
     * @param sessionState the session state.
     */
    public void setSessionState(SessionState sessionState) {
        data.setState(sessionState);;
    }

    @Override
    protected void additionalSanitize() {
    }

    @Override
    protected SessionStateData getData() {
        return data;
    }
}
