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

package com.microsoft.applicationinsights.framework.telemetries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;

/**
 * Created by yonisha on 6/21/2015.
 */
public class EventTelemetryItem extends TelemetryItem {

    public EventTelemetryItem() {
        super(DocumentType.Event);
    }

    public EventTelemetryItem(JSONObject json) throws URISyntaxException, JSONException {
        this();

        initRequestTelemetryItem(json);
    }

    @Override
    protected String[] getDefaultPropertiesToCompare() {
        return new String[0];
    }

    private void initRequestTelemetryItem(JSONObject json) throws URISyntaxException, JSONException {
        System.out.println("Converting JSON object to EventTelemetryItem");

        JSONObject context = json.getJSONObject("context");
        String operationId = context.getJSONObject("operation").getString("id");
        String operationName = context.getJSONObject("operation").getString("name");

        this.setProperty("operationId", operationId);
        this.setProperty("operationName", operationName);

        JSONObject custom = context.getJSONObject("custom");
        JSONArray dimensions = custom.getJSONArray("dimensions");

        String runId = null;
        for (int i = 0; i < dimensions.length(); i++) {
            JSONObject jsonObject = dimensions.getJSONObject(i);
            if (!jsonObject.isNull("runid")) {
                runId = jsonObject.getString("runid");
                break;
            }
        }

        this.setProperty("runId", runId);
    }
}
