package com.microsoft.applicationinsights.management.rest.model;

import com.microsoftopentechnologies.aad.adal4j.AuthenticationResult;

/**
 * Created by yonisha on 4/19/2015.
 */
public class Tenant {

    private String id;

    private AuthenticationResult authenticationToken;

    public Tenant() {
    }

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

    public String getName() {
        return getId().toString();
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
}
