package com.microsoft.applicationinsights.internal.authentication;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.identity.*;

public class HttpPipeLineWithAuthentication {
    private static final String APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE = "https://monitor.azure.com//.default";

    public static AuthenticationType authenticationType;

    public static String clientId;

    public static String keePassDatabasePath;

    public static HttpPipeline getHttpPipeLineWithAuthentication() {
        switch(authenticationType) {
            case UAMI:
                return getHttpPipeLineWithUAMI(clientId);
            case INTELLIJ:
                return getHttpPipeLineWithIntellij(keePassDatabasePath);
            case SAMI:
                return getHttpPipeLineWithSAMI();
            case VSCODE:
                return getHttpPipeLineWithVsCode();
            default:
                return getHttpPipeLineWithoutAuthentication();
        }
    }

    private static HttpPipeline getHttpPipeLineWithoutAuthentication() {
        HttpClient httpClient = HttpClient.createDefault();
        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(httpClient)
                .build();
        return httpPipeline;
    }

    private static HttpPipeline getHttpPipeLineWithVsCode() {
        HttpClient httpClient = HttpClient.createDefault();
        VisualStudioCodeCredential visualStudioCodeCredential = new VisualStudioCodeCredentialBuilder()
                .build();
        BearerTokenAuthenticationPolicy policy= new BearerTokenAuthenticationPolicy(visualStudioCodeCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(httpClient)
                .policies(policy)
                .build();
        return httpPipeline;
    }

    private static HttpPipeline getHttpPipeLineWithSAMI() {
        HttpClient httpClient = HttpClient.createDefault();
        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder()
                .build();
        BearerTokenAuthenticationPolicy policy= new BearerTokenAuthenticationPolicy(managedIdentityCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(httpClient)
                .policies(policy)
                .build();
        return httpPipeline;
    }

    private static HttpPipeline getHttpPipeLineWithIntellij(String keePassDatabasePath) {
        HttpClient httpClient = HttpClient.createDefault();
        IntelliJCredential intelliJCredential = new IntelliJCredentialBuilder()
                // KeePass configuration required only for Windows. No configuration needed for Linux / Mac
                .keePassDatabasePath(keePassDatabasePath)
                .build();
        BearerTokenAuthenticationPolicy policy= new BearerTokenAuthenticationPolicy(intelliJCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(httpClient)
                .policies(policy)
                .build();
        return httpPipeline;
    }

    private static HttpPipeline getHttpPipeLineWithUAMI(String clientId) {
        HttpClient httpClient = HttpClient.createDefault();
        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder()
                .clientId(clientId)
                .build();
        BearerTokenAuthenticationPolicy policy= new BearerTokenAuthenticationPolicy(managedIdentityCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(httpClient)
                .policies(policy)
                .build();
        return httpPipeline;
    }
}
