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

package com.microsoft.applicationinsights.management.rest.client;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;

import com.microsoft.applicationinsights.management.common.Logger;
import com.microsoftopentechnologies.aad.adal4j.AuthenticationResult;

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

    private static final Logger LOG = Logger.getLogger(RestClient.class.toString());
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

    public String executeGet(String path, String apiVersion) throws IOException, RestOperationException {
        LOG.info("Executing 'GET' operation for path {0}.\nAPI version: {1}.", path, apiVersion);

        return execute(path, HttpMethod.GET, null, apiVersion);
    }

    public String executePut(String path, String payload, String apiVersion) throws IOException, RestOperationException {
        LOG.info("Executing 'PUT' operation for path {0}.\nPayload: {1}.\nAPI version: {2}.", path, payload, apiVersion);

        return execute(path, HttpMethod.PUT, payload, apiVersion);
    }

    private String execute(String path, HttpMethod httpMethod, final String payload, String apiVersion) throws IOException, RestOperationException {
        HttpsURLConnection sslConnection = createSSLConnection(path, apiVersion, httpMethod, payload);

        LOG.info("Getting response.");
        int responseCode = sslConnection.getResponseCode();
        String responseMessage = sslConnection.getResponseMessage();

        String result = null;
        if (responseCode >= 200 && responseCode <= 299) {
            LOG.info("REST operation finished successfully. Response code: {0}, Status: {1}.", String.valueOf(responseCode), responseMessage);

            result = readStream(sslConnection.getInputStream(), true);
        } else {
            String errorMessage = readStream(sslConnection.getErrorStream());
            LOG.severe(
                    "REST operation failed with response code {0}, status {1}, error message: {2}", String.valueOf(responseCode), responseMessage, errorMessage);

            throw new RestOperationException(responseMessage, new OperationExceptionDetails(errorMessage));
        }

        return result;
    }

    // endregion Public methods

    // region Private Methods

    /**
     * Creates SSL connection.
     * @param path The url.
     * @param apiVersion The API version.
     * @param httpMethod The HTTP method to use.
     * @param payload
     * @return Https connection.
     */
    private HttpsURLConnection createSSLConnection(
            String path,
            String apiVersion,
            HttpMethod httpMethod, String payload) throws IOException {
        LOG.info("Generating SSL connection.");

        URL myUrl = new URL(new URL(AZURE_SERVICE_URI), path);
        HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();
        conn.setRequestMethod(httpMethod.toString());

        conn.addRequestProperty(USER_AGENT_HEADER, this.userAgent);
        conn.addRequestProperty(TELEMETRY_HEADER, this.userAgent);
        conn.addRequestProperty(X_MS_VERSION_HEADER, apiVersion);
        conn.addRequestProperty(ACCEPT_HEADER, JSON_CONTENT_TYPE);
        conn.addRequestProperty(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);
        conn.addRequestProperty(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + this.authenticationResult.getAccessToken());

        if (httpMethod.compareTo(HttpMethod.PUT) == 0) {
            conn.setDoOutput(true);
//            conn.setRequestProperty("Accept", "");

            if (payload != null) {
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(payload);
                wr.flush();
                wr.close();
            }
        }

        return conn;
    }

    private String readStream(InputStream is) throws IOException {
        return readStream(is, false);
    }

    private String readStream(InputStream is, boolean keepLines) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(is));
            String separator = System.getProperty("line.separator");
            StringBuilder response = new StringBuilder();

            String inputLine;
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
}
