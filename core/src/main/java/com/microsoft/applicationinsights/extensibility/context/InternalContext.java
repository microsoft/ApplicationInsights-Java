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

package com.microsoft.applicationinsights.extensibility.context;

import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class InternalContext {
    private final ConcurrentMap<String, String> tags;

    public InternalContext(ConcurrentMap<String, String> tags) {
        this.tags = tags;
    }

    public String getSdkVersion() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getInternalSdkVersion());
    }

    public void setSdkVersion(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getInternalSdkVersion(), version);
    }

    public String getAgentVersion() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getInternalAgentVersion());
    }

    public void setAgentVersion(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getInternalAgentVersion(), version);
    }

    public String getNodeName() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getInternalNodeName());
    }

    public void setNodeName(String nodeName) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getInternalNodeName(), nodeName);
    }
}