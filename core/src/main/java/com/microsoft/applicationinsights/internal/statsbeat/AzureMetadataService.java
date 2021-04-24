package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.internal.channel.common.LazyHttpClient;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.DEFAULT_STATSBEAT_INTERVAL;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RP_VM;

public final class AzureMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(AzureMetadataService.class);
    private static AzureMetadataService instance;
    private static final String API_VERSION = "api-version=2017-08-01"; // this version has the smallest payload.
    private static final String JSON_FORMAT = "format=json";
    private static final String BASE_URL = "http://169.254.169.254/metadata/instance/compute";

    private String endpoint;
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(AzureMetadataService.class));

    public static AzureMetadataService getInstance() {
        if (instance == null) {
            instance = new AzureMetadataService();
        }
        return instance;
    }

    private AzureMetadataService() {
        endpoint = String.format("%s?%s&%s", BASE_URL, API_VERSION, JSON_FORMAT);
        scheduledExecutor.scheduleAtFixedRate(new InvokeMetadataServiceTask(), DEFAULT_STATSBEAT_INTERVAL, DEFAULT_STATSBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    public static void parseJsonResponse(String response) throws IOException {
        if (response != null) {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<MetadataInstanceResponse> jsonAdapter = moshi.adapter(MetadataInstanceResponse.class);
            StatsbeatModule.getInstance().getAttachStatsbeat().updateMetadataInstance(jsonAdapter.fromJson(response));
        }
    }

    private class InvokeMetadataServiceTask implements Runnable {

        @Override
        public void run() {
            sendAsync();
        }

        private void sendAsync() {
            HttpGet request = new HttpGet(endpoint);
            request.addHeader("Metadata", "true");
            HttpResponse response;
            try {
                response = LazyHttpClient.getInstance().execute(request);
                if (response != null) {
                    StatsbeatModule.getInstance().getAttachStatsbeat().updateResourceProvider(RP_VM);
                    parseJsonResponse(response.toString());
                }
            } catch (Exception ex) {
                logger.debug("This is not running from an Azure VM or VMSS. Shut down AzureMetadataService scheduler.");
                scheduledExecutor.shutdown();
                return;
            }
        }


    }
}
