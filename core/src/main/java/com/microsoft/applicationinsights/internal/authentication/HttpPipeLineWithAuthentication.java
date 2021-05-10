package com.microsoft.applicationinsights.internal.authentication;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.identity.*;
import com.microsoft.applicationinsights.internal.system.SystemInformation;

public class HttpPipeLineWithAuthentication {
    private static final String APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE = "https://monitor.azure.com/.default";

    public volatile static AuthenticationType authenticationType;

    public volatile static String clientId;

    public volatile static String keePassDatabasePath;

    public volatile static String tenantId;

    public volatile static String clientSecret;

    public volatile static String authorityHost;

    public static HttpPipeline newHttpPipeLineWithAuthentication() {

        if(authenticationType == null) return getHttpPipeLineWithoutAuthentication();

        switch(authenticationType) {
            case UAMI:
                return getHttpPipeLineWithUAMI(clientId);
            case INTELLIJ:
                return getHttpPipeLineWithIntellij(keePassDatabasePath);
            case SAMI:
                return getHttpPipeLineWithSAMI();
            case VSCODE:
                return getHttpPipeLineWithVsCode();
            case CLIENTSECRET:
                return getHttpPipeLineWithClientSecret();
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
        // KeePass configuration required only for Windows. No configuration needed for Linux / Mac
        IntelliJCredential intelliJCredential = SystemInformation.INSTANCE.isWindows() ? new IntelliJCredentialBuilder()
                .keePassDatabasePath(keePassDatabasePath)
                .build() : new IntelliJCredentialBuilder()
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

    private static HttpPipeline getHttpPipeLineWithClientSecret() {
        HttpClient httpClient = HttpClient.createDefault();
        ClientSecretCredential credential = authorityHost == null ? new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientSecret(clientSecret)
                .clientId(clientId)
                .build() : new ClientSecretCredentialBuilder()
                .authorityHost(authorityHost)
                .tenantId(tenantId)
                .clientSecret(clientSecret)
                .clientId(clientId)
                .build();
        BearerTokenAuthenticationPolicy policy= new BearerTokenAuthenticationPolicy(credential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(httpClient)
                .policies(policy)
                .build();
        return httpPipeline;
    }
}
