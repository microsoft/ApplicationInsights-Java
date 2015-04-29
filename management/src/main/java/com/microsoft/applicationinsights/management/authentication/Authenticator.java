package com.microsoft.applicationinsights.management.authentication;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoftopentechnologies.aad.adal4j.AuthenticationContext;
import com.microsoftopentechnologies.aad.adal4j.AuthenticationResult;
import com.microsoftopentechnologies.aad.adal4j.PromptValue;

import java.io.IOException;

/**
 * Created by yonisha on 4/19/2015.
 */
public class Authenticator {
    public static AuthenticationResult getAuthenticationResult() throws IOException, InterruptedException {
        String authority = Settings.getAdAuthority();
        String tenantName = Settings.getTenant();
        String resource = Settings.getResource();
        String clientID = Settings.getClientId();
        String redirectURI = Settings.getRedirectURI();

        AuthenticationContext context = new AuthenticationContext(authority);
        ListenableFuture<AuthenticationResult> future = context.acquireTokenInteractiveAsync(
                tenantName,
                resource,
                clientID,
                redirectURI,
                PromptValue.login);

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
            Thread.sleep(5000);
        }

        return result[0];
    }
}
