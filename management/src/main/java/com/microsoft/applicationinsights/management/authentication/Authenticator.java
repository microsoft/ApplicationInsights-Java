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

package com.microsoft.applicationinsights.management.authentication;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.applicationinsights.management.rest.model.Tenant;
import com.microsoftopentechnologies.aad.adal4j.AuthenticationContext;
import com.microsoftopentechnologies.aad.adal4j.AuthenticationResult;
import com.microsoftopentechnologies.aad.adal4j.PromptValue;

import java.io.IOException;

/**
 * Created by yonisha on 4/19/2015.
 */
public class Authenticator {
    public static AuthenticationResult getAuthenticationResult() throws IOException {
        String commonTenant = Settings.getTenant();

        return getAuthenticationResultForTenant(commonTenant);
    }

    public static AuthenticationResult getAuthenticationResultForTenant(String tenant) throws IOException {
        // We first try to login without prompt the user for a user/password.
        AuthenticationResult authenticationResult = getAuthenticationResultForTenantInternal(tenant, PromptValue.attemptNone);

        // If we fail to authenticate, we prompt the user for username & password.
        if (authenticationResult == null){
            authenticationResult = getAuthenticationResultForTenantInternal(tenant, PromptValue.login);
        }

        return authenticationResult;
    }

    public static AuthenticationResult getAuthenticationResultForTenantInternal(String tenant, String promptValue) throws IOException {
        String authority = Settings.getAdAuthority();
        String resource = Settings.getResource();
        String clientID = Settings.getClientId();
        String redirectURI = Settings.getRedirectURI();

        AuthenticationContext context = new AuthenticationContext(authority);
        ListenableFuture<AuthenticationResult> future = context.acquireTokenInteractiveAsync(
                tenant,
                resource,
                clientID,
                redirectURI,
                promptValue);

        final AuthenticationResult[] result = {null};
        Futures.addCallback(future, new FutureCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                System.out.println("Authentication completed.");
                result[0] = authenticationResult;
            }

            @Override
            public void onFailure(Throwable t) {
                System.out.println("Authentication failed.");
                System.out.println(t.toString());
            }
        });

        System.out.println("Waiting to complete authentication...");
        while (!future.isDone()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }

        return result[0];
    }
}
