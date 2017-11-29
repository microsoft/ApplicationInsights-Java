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

package com.microsoft.applicationinsights.web.internal.correlation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class responsible to store the correlation context information.
 */
public class CorrelationContext {

    /**
     * Stores the contents of Correlation-Context headers found in the incoming request.
     */
    private final List<String> incomingHeaderValues;


    /**
     * Stores the correlation context as mappings.
     */
    private final Map<String, String> mappings; 

    /**
     * Stores the context as a string.
     */
    private final StringBuilder contextAsString;

    public CorrelationContext() {
        this.incomingHeaderValues = new ArrayList<String>();
        this.mappings = new HashMap<String, String>();
        this.contextAsString = new StringBuilder();
    }

    /**
     * Gets the correlation context key-value pairs.
     */
    public Map<String, String> getMappings() {
        return this.mappings;
    }

    /**
     * Gets the correlation context headers for the request context.
     */
    public List<String> getHeaderValues() {
        return this.incomingHeaderValues;
    }

    /**
     * Appends content to the correlation context.
     */
    public void append(String content) {
        if (this.contextAsString.length() > 0) {
            this.contextAsString.append(",");
        }
        this.contextAsString.append(content);
    }

    /**
     * Returns a single string for the whole correlation context.
     */
    @Override
    public String toString() {
        return this.contextAsString.toString();
    }
}