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
import java.util.Hashtable;

/**
 * Created by moralt on 05/05/2015.
 */
public class RequestTelemetryItem extends TelemetryItem {
    private static final String[] propertiesToCompare = new String[] {
        "port",
        "responseCode",
        "uri",
        "sessionId",
        "userId"
    };

    public RequestTelemetryItem() {
        super(DocumentType.Requests);
    }

    public RequestTelemetryItem(JSONObject json) throws URISyntaxException, JSONException {
        this();

        initRequestTelemetryItem(json);
    }

    @Override
    protected String[] getDefaultPropertiesToCompare() {
        return propertiesToCompare;
    }

    /**
     * Converts JSON object to Request TelemetryItem
     * @param json The JSON object
     */
    private void initRequestTelemetryItem(JSONObject json) throws URISyntaxException, JSONException {
        System.out.println("Converting JSON object to RequestTelemetryItem");
        JSONObject requestProperties = json.getJSONArray("request").getJSONObject(0);

        String address       = requestProperties.getString("url");
        Integer port         = requestProperties.getJSONObject("urlData").getInt("port");
        Integer responseCode = requestProperties.getInt("responseCode");

        JSONArray parameters = requestProperties.getJSONObject("urlData").getJSONArray("queryParameters");
        Hashtable<String, String> queryParameters = new Hashtable<String, String>();
        for (int i = 0; i < parameters.length(); ++i) {
            JSONObject parameterPair = parameters.getJSONObject(i);
            String name  = parameterPair.getString("parameter");
            String value = parameterPair.getString("value");
            queryParameters.put(name, value);
        }

        JSONObject context = json.getJSONObject("context");
        String sessionId = context.getJSONObject("session").getString("id");
        String userId = context.getJSONObject("user").getString("anonId");

        this.setProperty("uri", address);
        this.setProperty("port", port.toString());
        this.setProperty("responseCode", responseCode.toString());
        this.setProperty("userId", userId);
        this.setProperty("sessionId", sessionId);

        for (String key : queryParameters.keySet()) {
            this.setProperty("queryParameter." + key, queryParameters.get(key));
        }
    }
}
