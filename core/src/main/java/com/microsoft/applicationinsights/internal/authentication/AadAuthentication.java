package com.microsoft.applicationinsights.internal.authentication;

import com.azure.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.identity.VisualStudioCodeCredential;
import com.azure.identity.VisualStudioCodeCredentialBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AadAuthentication {
    private static final String APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE = "https://monitor.azure.com//.default";

    public final AuthenticationType authenticationType;
    public final @Nullable String clientId;
    public final @Nullable String tenantId;
    public final @Nullable String clientSecret;
    public final @Nullable String authorityHost;

    public AadAuthentication(AuthenticationType authenticationType, @Nullable String clientId,
                             @Nullable String tenantId, @Nullable String clientSecret, @Nullable String authorityHost) {
        this.authenticationType = authenticationType;
        this.clientId = clientId;
        this.tenantId = tenantId;
        this.clientSecret = clientSecret;
        this.authorityHost = authorityHost;
    }

    public HttpPipelinePolicy getAuthenticationPolicy() {
        switch (authenticationType) {
            case UAMI:
                return getAuthenticationPolicyWithUAMI();
            case SAMI:
                return getAuthenticationPolicyWithSAMI();
            case VSCODE:
                return getAuthenticationPolicyWithVsCode();
            case CLIENTSECRET:
                return getAuthenticationPolicyWithClientSecret();
        }
        throw new IllegalStateException("Invalid Authentication Type used in AAD Authentication: " + authenticationType);
    }

    private HttpPipelinePolicy getAuthenticationPolicyWithUAMI() {
        ManagedIdentityCredentialBuilder managedIdentityCredential = new ManagedIdentityCredentialBuilder()
                .clientId(clientId);
        return new BearerTokenAuthenticationPolicy(managedIdentityCredential.build(), APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
    }

    private HttpPipelinePolicy getAuthenticationPolicyWithClientSecret() {
        ClientSecretCredentialBuilder credential = new ClientSecretCredentialBuilder()
            .tenantId(tenantId)
            .clientSecret(clientSecret)
            .clientId(clientId);
        if (authorityHost != null) {
            credential.authorityHost(authorityHost);
        }
        return new BearerTokenAuthenticationPolicy(credential.build(), APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
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
}
