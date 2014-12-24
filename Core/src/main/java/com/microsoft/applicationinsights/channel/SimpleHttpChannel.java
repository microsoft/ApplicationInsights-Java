package com.microsoft.applicationinsights.channel;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.implementation.JsonWriter;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * A simple HTTP channel, using no buffering, batching, or asynchrony.
 */
public class SimpleHttpChannel implements TelemetryChannel
{
    @Override
    public boolean isDeveloperMode()
    {
        return developerMode;
    }

    @Override
    public void setDeveloperMode(boolean value)
    {
        developerMode = value;
    }

    @Override
    public void send(Telemetry item)
    {
        try
        {
            // Establish the payload.
            StringWriter writer = new StringWriter();
            item.serialize(new JsonWriter(writer));

            // Send it.

            String payload = writer.toString();

            if (developerMode) System.out.println(payload);

            HttpPost request = new HttpPost("https://dc.services.visualstudio.com/v2/track");
            StringEntity body = new StringEntity(payload, ContentType.create("application/x-json-stream"));
            request.setEntity(body);

            CloseableHttpClient httpClient = HttpClients.createDefault();

            CloseableHttpResponse response = null;
            try
            {
                response = httpClient.execute(request);
                HttpEntity respEntity = response.getEntity();
                if (respEntity != null)
                    respEntity.getContent().close();

                if (developerMode) System.out.println("Status: " + response.getStatusLine());
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace(System.err);
                try
                {
                    if (response != null)
                    {
                        response.close();
                    }
                }
                catch (IOException ioeIn)
                {
                    ioeIn.printStackTrace(System.err);
                }
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace(System.err);
        }
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
    }

    private boolean developerMode = false;
}
