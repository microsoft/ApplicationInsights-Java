package com.microsoft.applicationinsights.management.rest.operations;

import com.microsoft.applicationinsights.management.rest.client.Client;

import java.io.IOException;

/**
 * Created by yonisha on 4/19/2015.
 *
 * Interface that all REST operations need to implement.
 */
public interface RestOperation<T> {
    T execute(Client restClient) throws IOException, AzureCmdException;
}