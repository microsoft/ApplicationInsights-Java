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

import java.util.Date;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class UserContext {
    private final ConcurrentMap<String,String> tags;

    public UserContext(ConcurrentMap<String, String> tags)
    {
        this.tags = tags;
    }

    public String getId() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getUserId());
    }

    public void setId(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserId(), version);
    }

    String getAccountId() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getUserAccountId());
    }

    public void setAccountId(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserAccountId(), version);
    }

    String getUserAgent() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getUserAgent());
    }

    public void setUserAgent(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserAgent(), version);
    }

    public Date getAcquisitionDate() {
        return MapUtil.getDateValueOrNull(tags, ContextTagKeys.getKeys().getUserAccountAcquisitionDate());
    }

    public void setAcquisitionDate(Date version) {
        MapUtil.setDateValueOrRemove(tags, ContextTagKeys.getKeys().getUserAccountAcquisitionDate(), version);
    }
}