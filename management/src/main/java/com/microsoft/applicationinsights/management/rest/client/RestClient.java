package com.microsoft.applicationinsights.management.rest.client;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.Scanner;
import com.microsoft.applicationinsights.management.rest.operations.AzureCmdException;
import com.microsoftopentechnologies.aad.adal4j.AuthenticationResult;
import net.minidev.json.JSONObject;

/**
 * Created by yonisha on 4/19/2015.
 */
public class RestClient implements Client {

    // region Consts

    private static final String AUTHORIZATION_VALUE_PREFIX = "Bearer ";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String AZURE_SERVICE_URI = "https://management.azure.com/";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String TELEMETRY_HEADER = "X-ClientService-ClientTag";
    private static final String X_MS_VERSION_HEADER = "x-ms-version";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    // endregion Consts

    // region Members

    private String userAgent;
    private AuthenticationResult authenticationResult;

    // endregion Members

    // region Ctor

    public RestClient(AuthenticationResult authenticationResult) {
        this.authenticationResult = authenticationResult;
    }

    public RestClient(AuthenticationResult authenticationResult, String userAgent) {
        this(authenticationResult);

        this.userAgent = userAgent;
    }

    // endregion Ctor

    // region Public methods

    // TODO: add expected code, method??

    public String executeGet(String path, String apiVersion) throws IOException, AzureCmdException {
        return execute(path, HttpMethod.GET, null, apiVersion);
    }

    public String executePut(String path, String payload, String apiVersion) throws IOException, AzureCmdException {
        return execute(path, HttpMethod.PUT, payload, apiVersion);
    }

    private String execute(String path, HttpMethod httpMethod, final String payload, String apiVersion) throws IOException, AzureCmdException {

        final HttpMethod method = httpMethod;
        AzureRestCallbackAdapter<String> callback = new AzureRestCallbackAdapter<String>() {
            @Override
            public int apply(HttpsURLConnection sslConnection) throws IOException {
                if (method.compareTo(HttpMethod.PUT) == 0) {
                    sslConnection.setDoOutput(true);
                    sslConnection.setRequestProperty("Accept", "");

                    if (payload != null) {
                        DataOutputStream wr = new DataOutputStream(sslConnection.getOutputStream());
                        wr.writeBytes(payload);
                        wr.flush();
                        wr.close();
                    }
                }

                // TODO: wrap it up to contain all (code, message, error stream etc.)
                int responseCode = sslConnection.getResponseCode();
                String errorMessage = sslConnection.getResponseMessage();

                System.out.println(errorMessage);

                InputStream errorInputStream = sslConnection.getErrorStream();

                // TODO: remove
                if (errorInputStream != null) {
                    String errorStream = new Scanner(errorInputStream, "UTF-8").useDelimiter("\\A").next();
                    System.out.println(errorStream);
                }

                if (sslConnection.getResponseCode() < 400) {
                    setResult(readStream(sslConnection.getInputStream(), true));
                }

                // TODO: else?

                return responseCode;
            }
        };

        executeWithSSLConnectionInternal(path, apiVersion, httpMethod, callback);

        if (!callback.isOk()) {
            throw callback.getError();
        }

        return callback.getResult();
    }

    /**
     * Execute the operation with SSL connection.
     * @param path The operation path.
     * @param apiVersion The operation API version.
     * @param httpMethod The HTTP method to use.
     * @param callback The callback to use for getting the result.
     */
    public <T> void executeWithSSLConnection(
        String path,
        String apiVersion,
        HttpMethod httpMethod,
        AzureRestCallback<T> callback) throws IOException, AzureCmdException {

        executeWithSSLConnectionInternal(path, apiVersion, httpMethod, callback);
    }
    // endregion Public methods

    // region Private Methods

    /**
     * Execute the operation with SSL connection.
     * @param path The operation path.
     * @param apiVersion The operation API version.
     * @param httpMethod The HTTP method to use.
     * @param callback The callback to use for getting the result.
     */
    private <T> void executeWithSSLConnectionInternal(
            String path,
            String apiVersion,
            HttpMethod httpMethod,
            AzureRestCallback<T> callback) throws IOException, AzureCmdException {

        HttpsURLConnection sslConnection = createSSLConnection(path, apiVersion, httpMethod);
        int response = callback.apply(sslConnection);

        if (response < 200 || response > 299) {
            throw new AzureCmdException("Error connecting to service", readStream(sslConnection.getErrorStream()));
        }
    }

    /**
     * Creates SSL connection.
     * @param path The url.
     * @param apiVersion The API version.
     * @param httpMethod The HTTP method to use.
     * @return Https connection.
     */
    private HttpsURLConnection createSSLConnection(
            String path,
            String apiVersion,
            HttpMethod httpMethod) throws IOException {

        URL myUrl = new URL(new URL(AZURE_SERVICE_URI), path);
        HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();
        conn.setRequestMethod(httpMethod.toString());

        if (httpMethod.compareTo(HttpMethod.PUT) == 0) {
            conn.setDoOutput(true);
        }

        conn.addRequestProperty(USER_AGENT_HEADER, this.userAgent);
        conn.addRequestProperty(TELEMETRY_HEADER, this.userAgent);
        conn.addRequestProperty(X_MS_VERSION_HEADER, apiVersion);
        conn.addRequestProperty(ACCEPT_HEADER, JSON_CONTENT_TYPE);
        conn.addRequestProperty(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);
        conn.addRequestProperty(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + this.authenticationResult.getAccessToken());

        return conn;
    }

    private String readStream(InputStream is) throws IOException {
        return readStream(is, false);
    }

    private String readStream(InputStream is, boolean keepLines) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(
                    new InputStreamReader(is));
            String inputLine;
            String separator = System.getProperty("line.separator");
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
                if (keepLines) {
                    response.append(separator);
                }
            }
            in.close();
            in = null;
            return response.toString();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    // endregion Private Methods

    // region Private Classes

    interface AzureRestCallback<T> {
        //@Nullable
        int apply(HttpsURLConnection sslConnection) throws IOException;

        T getResult();

        void setResult(T result);

        AzureCmdException getError();

        void setError(AzureCmdException throwable);

        boolean isOk();

        @Override
        boolean equals(java.lang.Object o);
        //boolean equals(@Nullable java.lang.Object o);
    }

    abstract static class AzureRestCallbackAdapter<T> implements AzureRestCallback<T> {
        private T result;
        private AzureCmdException azureError = null;

        @Override
        public T getResult() {
            return result;
        }

        @Override
        public void setResult(T result) {
            this.result = result;
        }

        @Override
        public AzureCmdException getError() {
            return azureError;
        }

        @Override
        public void setError(AzureCmdException throwable) {
            this.azureError = throwable;
        }

        @Override
        public boolean isOk() {
            return azureError == null;
        }
    }

    // endregion Private Classes
}
