package com.microsoft.applicationinsights.internal.authentication;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.policy.BearerTokenAuthenticationPolicy;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.http.policy.HttpLoggingPolicy;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.IntelliJCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.identity.VisualStudioCodeCredential;
import com.azure.identity.VisualStudioCodeCredentialBuilder;
import com.microsoft.applicationinsights.internal.channel.common.LazyAzureHttpClient;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AadAuthentication {
    private static final String APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE = "https://monitor.azure.com//.default";

    private static volatile AadAuthentication instance;


    public final @Nullable AuthenticationType authenticationType;
    public final @Nullable String clientId;
    public final @Nullable String keePassDatabasePath;
    public final @Nullable String tenantId;
    public final @Nullable String clientSecret;
    public final @Nullable String authorityHost;

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
                return getAuthenticationPolicyWithUAMI();
            case INTELLIJ:
                return getAuthenticationPolicyWithIntellij();
            case SAMI:
                return getAuthenticationPolicyWithSAMI();
            case VSCODE:
                return getAuthenticationPolicyWithVsCode();
            case CLIENTSECRET:
                return getAuthenticationPolicyWithClientSecret();
            default:
                throw new IllegalStateException("Invalid Authentication Type used in AAD Authentication: " + authenticationType);
        }
    }

    private HttpPipelinePolicy getAuthenticationPolicyWithIntellij() {
        IntelliJCredentialBuilder intelliJCredential = new IntelliJCredentialBuilder();
        // KeePass configuration required only for Windows. No configuration needed for Linux / Mac
        if(SystemInformation.INSTANCE.isWindows() && keePassDatabasePath != null) {
            intelliJCredential.keePassDatabasePath(keePassDatabasePath);
        }
        return new BearerTokenAuthenticationPolicy(intelliJCredential.build(), APPLICATIONINSIGHTS_AUTHENTICATION_SCOPE);
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
    
    public HttpPipeline newHttpPipeLineWithAuthentication() {
        List<HttpPipelinePolicy> policies = new ArrayList<>();
        // Retry policy for failed requests
        policies.add(new RetryPolicy());
        HttpPipelinePolicy authenticationPolicy = getAuthenticationPolicy();
        HttpClient httpClient = LazyAzureHttpClient.getInstance();
        HttpPipelineBuilder pipelineBuilder = new HttpPipelineBuilder().httpClient(httpClient);
        if(authenticationPolicy != null) {
            policies.add(authenticationPolicy);
        }
        // Add Logging Policy
        policies.add(new HttpLoggingPolicy(new HttpLogOptions()));
        pipelineBuilder.policies(policies.toArray(new HttpPipelinePolicy[0]));
        return pipelineBuilder.build();
    }
}
