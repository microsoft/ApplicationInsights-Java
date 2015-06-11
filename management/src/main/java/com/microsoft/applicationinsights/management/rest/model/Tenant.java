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
import com.microsoftopentechnologies.aad.adal4j.AuthenticationResult;

/**
 * Created by yonisha on 4/19/2015.
 *
 * This class represents an Azure tenant.
 * An azure account (represented by e-mail) can have several tenants.
 */
public class Tenant {

    private String id;

    private AuthenticationResult authenticationToken;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    public AuthenticationResult getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(AuthenticationResult authenticationToken) {
        this.authenticationToken = authenticationToken;
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

        Tenant other = (Tenant) obj;
        return id != null && id.equals(other.getId());
    }

    public static Tenant fromJSONObject(JsonObject tenantJson) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantJson.get("tenantId").getAsString());

        return tenant;
    }
}
