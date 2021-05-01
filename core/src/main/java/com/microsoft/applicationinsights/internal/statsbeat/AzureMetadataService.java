package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.internal.channel.common.LazyHttpClient;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonEncodingException;
import com.squareup.moshi.Moshi;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AzureMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(AzureMetadataService.class);
    private static final AzureMetadataService INSTANCE = new AzureMetadataService();
    private static final String API_VERSION = "api-version=2017-08-01"; // this version has the smallest payload.
    private static final String JSON_FORMAT = "format=json";
    private static final String BASE_URL = "http://169.254.169.254/metadata/instance/compute";

    private static final String endpoint = BASE_URL + "?" + API_VERSION + "&" + JSON_FORMAT;
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(AzureMetadataService.class));
    private JsonAdapter<MetadataInstanceResponse> jsonAdapter;

    public static AzureMetadataService getInstance() {
        return INSTANCE;
    }

    public void initialize(long interval) {
        jsonAdapter = new Moshi.Builder().build().adapter(MetadataInstanceResponse.class);
        scheduledExecutor.scheduleAtFixedRate(new InvokeMetadataServiceTask(), interval, interval, TimeUnit.SECONDS);
    }

    public void parseJsonResponse(String response) throws IOException {
        if (response != null) {
            StatsbeatModule.getInstance().getAttachStatsbeat().updateMetadataInstance(jsonAdapter.fromJson(response));
        }
    }

    private class InvokeMetadataServiceTask implements Runnable {

        @Override
        public void run() {
            HttpGet request = new HttpGet(endpoint);
            request.addHeader("Metadata", "true");
            try {
                HttpResponse response = LazyHttpClient.getInstance().execute(request);
                if (response != null) {
                    AzureMetadataService.this.parseJsonResponse(response.toString());
                }
            } catch (JsonEncodingException jsonEncodingException) {
                // When it's not VM/VMSS, server does not return json back, and instead it returns text like the following:
                // "<br />Error: NetworkUnreachable (0x2743). <br />System.Net.Sockets.SocketException A socket operation was attempted to an unreachable network 169.254.169.254:80".
                logger.debug("This is not running from an Azure VM or VMSS. Shut down AzureMetadataService scheduler.");
                scheduledExecutor.shutdown();
                return;
            } catch (Exception ex) {
                // TODO add backoff and retry if it's a sporadic failure
                logger.debug("Fail to query Azure Metadata Service. {}", ex);
            }
        }
    }
}
