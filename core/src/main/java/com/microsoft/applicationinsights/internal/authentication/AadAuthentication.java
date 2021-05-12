package com.microsoft.applicationinsights.internal.authentication;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.identity.*;
import com.microsoft.applicationinsights.internal.system.SystemInformation;

public class AadAuthentication {
    private static final String APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE = "https://monitor.azure.com/.default";

    private static volatile AadAuthentication instance;

    public final AuthenticationType authenticationType;
    public final String clientId;
    public final String keePassDatabasePath;
    public final String tenantId;
    public final String clientSecret;
    public final String authorityHost;

    public static void init(AuthenticationType authenticationType, String clientId, String keePassDatabasePath,
                            String tenantId, String clientSecret, String authorityHost) {
        AadAuthentication.instance = new AadAuthentication(authenticationType, clientId, keePassDatabasePath, tenantId,
                clientSecret, authorityHost);
    }

    public static AadAuthentication getInstance() {
        if (instance == null) {
            throw new IllegalStateException("must init before using");
        }
        return instance;
    }

    private AadAuthentication(AuthenticationType authenticationType, String clientId, String keePassDatabasePath,
                             String tenantId, String clientSecret, String authorityHost) {
        this.authenticationType = authenticationType;
        this.clientId = clientId;
        this.keePassDatabasePath = keePassDatabasePath;
        this.tenantId = tenantId;
        this.clientSecret = clientSecret;
        this.authorityHost = authorityHost;
    }

    public HttpPipelinePolicy getAuthenticationPolicy() {
        if (authenticationType == null) return null;
        switch (authenticationType) {
            case UAMI:
                return getAuthenticationPolicyWithUAMI(clientId);
            case INTELLIJ:
                return getAuthenticationPolicyWithIntellij(keePassDatabasePath);
            case SAMI:
                return getAuthenticationPolicyWithSAMI();
            case VSCODE:
                return getAuthenticationPolicyWithVsCode();
            case CLIENTSECRET:
                return getAuthenticationPolicyWithClientSecret();
            default:
                return null;
        }
    }

    private HttpPipelinePolicy getAuthenticationPolicyWithClientSecret() {
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
        return new BearerTokenAuthenticationPolicy(credential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
    }

    private static HttpPipelinePolicy getAuthenticationPolicyWithVsCode() {
        VisualStudioCodeCredential visualStudioCodeCredential = new VisualStudioCodeCredentialBuilder()
                .build();
        return new BearerTokenAuthenticationPolicy(visualStudioCodeCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
    }

    private static HttpPipelinePolicy getAuthenticationPolicyWithSAMI() {
        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder()
                .build();
        return new BearerTokenAuthenticationPolicy(managedIdentityCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
    }

    private static HttpPipelinePolicy getAuthenticationPolicyWithIntellij(String keePassDatabasePath) {
        // KeePass configuration required only for Windows. No configuration needed for Linux / Mac
        IntelliJCredential intelliJCredential = SystemInformation.INSTANCE.isWindows() ? new IntelliJCredentialBuilder()
                .keePassDatabasePath(keePassDatabasePath)
                .build() : new IntelliJCredentialBuilder()
                .build();
        return new BearerTokenAuthenticationPolicy(intelliJCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
    }

    private static HttpPipelinePolicy getAuthenticationPolicyWithUAMI(String clientId) {
        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder()
                .clientId(clientId)
                .build();
        return new BearerTokenAuthenticationPolicy(managedIdentityCredential, APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
    }

    public HttpPipeline newHttpPipeLineWithAuthentication() {

        HttpPipelinePolicy authenticationPolicy = getAuthenticationPolicy();
        HttpClient httpClient = HttpClient.createDefault();

        if (authenticationPolicy == null) {
            return new HttpPipelineBuilder()
                    .httpClient(httpClient)
                    .build();
        }

        return new HttpPipelineBuilder()
                .httpClient(httpClient)
                .policies(authenticationPolicy)
                .build();
    }
}
