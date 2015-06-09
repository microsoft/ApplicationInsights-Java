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

package com.microsoft.applicationinsights.management.rest.model;

import com.google.gson.JsonObject;

/**
 * Created by yonisha on 4/19/2015.
 * A class represents a subscription in Azure.
 */
public class Subscription {
    private String id;
    private String name;

    /**
     * Gets the subscription ID.
     * @return The subscription ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the subscription ID.
     * @param id The subscription ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the subscription name.
     * @return The subscription name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the subscription name.
     * @param name The subscription name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Converts the given subscription JSON object to Subscription object.
     * @param subscriptionJson The subscription JSON object.
     * @return Subscription object.
     */
    public static Subscription fromJSONObject(JsonObject subscriptionJson) {
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionJson.get("subscriptionId").getAsString());
        subscription.setName(subscriptionJson.get("displayName").getAsString());

        return subscription;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        Subscription other = (Subscription) obj;

        return id != null && id.equals(other.getId());
    }
}
