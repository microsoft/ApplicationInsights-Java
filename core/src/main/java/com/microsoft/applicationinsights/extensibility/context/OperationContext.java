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

import java.util.Map;
import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class OperationContext {
    private final Map<String, String> tags;

    /**
     * Constructs new OperationContext object with the given tags.
     * @param tags The OperationContext tags.
     */
    public OperationContext(Map<String, String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the operation id.
     * @return Operation id.
     */
    public String getId() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getOperationId());
    }

    /**
     * Sets the operation id.
     * @param id The operation id.
     */
    public void setId(String id) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getOperationId(), id);
    }

    /**
     * Gets the operation name.
     * @return Operation name.
     */
    public String getName() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getOperationName());
    }

    /**
     * Sets the operation name.
     * @param name Operation name.
     */
    public void setName(String name) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getOperationName(), name);
    }
}