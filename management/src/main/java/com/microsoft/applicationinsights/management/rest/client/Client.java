package com.microsoft.applicationinsights.management.rest.client;

import java.io.IOException;
import com.microsoft.applicationinsights.management.rest.client.HttpMethod;
import com.microsoft.applicationinsights.management.rest.operations.AzureCmdException;

/**
 * Created by yonisha on 4/19/2015.
 */
public interface Client {
    String executeGet(String path, String apiVersion) throws IOException, AzureCmdException;
    String executePut(String path, String payload, String apiVersion) throws IOException, AzureCmdException;
}
