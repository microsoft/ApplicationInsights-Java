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

package com.microsoft.applicationinsights.extensibility.context;

import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class SessionContext {
    private final ConcurrentMap<String, String> tags;

    /**
     * Constructs a SessionContext objects with the given tags.
     * @param tags The tags
     */
    public SessionContext(ConcurrentMap<String, String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the session ID
     * @return Session ID
     */
    public String getId() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getSessionId());
    }

    /**
     * Sets the session ID.
     * @param id the session ID.
     */
    public void setId(String id) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getSessionId(), id);
    }

    /**
     * Gets a value indicating whether it is the first session.
     * @return True if first session, false otherwise.
     */
    Boolean getIsFirst() {
        return MapUtil.getBoolValueOrNull(tags, ContextTagKeys.getKeys().getSessionIsFirst());
    }

    /**
     * Sets whether it is the first session.
     * @param isFirst a value indicating whether it is the first session.
     */
    public void setIsFirst(Boolean isFirst) {
        MapUtil.setBoolValueOrRemove(tags, ContextTagKeys.getKeys().getSessionIsFirst(), isFirst);
    }

    /**
     * Gets a value indicating whether it is a new session.
     * @return True if new session, false otherwise.
     */
    public Boolean getIsNewSession() {
        return MapUtil.getBoolValueOrNull(tags, ContextTagKeys.getKeys().getSessionIsNew());
    }

    /**
     * Sets a value indicating whether it is a new session.
     * @param isNewSession A value indicating whether it is a new session.
     */
    public void setIsNewSession(Boolean isNewSession) {
        MapUtil.setBoolValueOrRemove(tags, ContextTagKeys.getKeys().getSessionIsNew(), isNewSession);
    }
}