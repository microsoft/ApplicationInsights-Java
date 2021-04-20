package com.microsoft.applicationinsights.internal.channel.common;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import javax.net.ssl.SSLHandshakeException;

import com.microsoft.applicationinsights.internal.util.SSLOptionsUtil;
import com.microsoft.applicationinsights.internal.util.SSLUtil;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyHttpClient extends CloseableHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(LazyHttpClient.class);

    private static final LazyHttpClient INSTANCE = new LazyHttpClient();

    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 200;
    private static final int REQUEST_TIMEOUT_IN_MILLIS = 60000;
    private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 20;

    public static volatile CountDownLatch safeToInitLatch;
    public static volatile HttpHost proxy;

    private final Object lock = new Object();

    @GuardedBy("lock")
    private RuntimeException initException;
    @GuardedBy("lock")
    private CloseableHttpClient delegate;

    public static CloseableHttpClient getInstance() {
        return INSTANCE;
    }

    private LazyHttpClient() {
    }

    private CloseableHttpClient getDelegate() {
        synchronized (lock) {
            if (delegate != null) {
                return delegate;
            }
            if (initException != null) {
                throw initException;
            }
            try {
                delegate = init();
            } catch (RuntimeException e) {
                initException = e;
            }
            return delegate;
        }
    }

    public static void dispose(HttpResponse response) {
        try {
            if (response != null) {
                ((CloseableHttpResponse) response).close();
            }
        } catch (IOException e) {
            logger.error("Failed to send or failed to close response, exception: {}", e.toString());
        }
    }

    private static CloseableHttpClient init() {
        if (safeToInitLatch != null) {
            try {
                // this is used to delay SSL initialization because SSL initialization triggers loading of
                // java.util.logging (starting with Java 8u231)
                // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized
                //
                // limit wait time to 2 minutes in case agent incorrectly anticipated needing to delay JUL
                // initialization in an environment that never loads JUL
                safeToInitLatch.await(2, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        String[] allowedProtocols = SSLOptionsUtil.getAllowedProtocols();
        PoolingHttpClientConnectionManager cm =
                new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https",
                                new SSLConnectionSocketFactory(SSLContexts.createDefault(), allowedProtocols, null,
                                        SSLConnectionSocketFactory.getDefaultHostnameVerifier()))
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .build());
        cm.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
        cm.setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                .build());
        HttpClientBuilder builder = HttpClients.custom()
                // need to set empty User-Agent, otherwise Breeze ingestion service will put the Apache HttpClient User-Agent header
                // into the client_Browser field for all telemetry that doesn't explicitly set it's own UserAgent
                // (ideally Breeze would only have this behavior for ingestion directly from browsers)
                // (not setting User-Agent header at all would be a good option, but Apache HttpClient doesn't have a simple way to do that)
                .setUserAgent("")
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                        .setSocketTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                        .setConnectTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                        .build())
                .setConnectionManager(cm)
                .useSystemProperties();
        if (proxy != null) {
            builder.setProxy(proxy);
        }
        return builder.build();
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        try {
            return getDelegate().execute(target, request, context);
        } catch (SSLHandshakeException e) {
            String completeUrl = "https://" + target.getHostName();
            throw SSLUtil.newSSLFriendlyException(completeUrl);
        }
    }

    @Override
    public void close() throws IOException {
        getDelegate().close();
    }

    @Override
    public HttpParams getParams() {
        return getDelegate().getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return getDelegate().getConnectionManager();
    }
}
