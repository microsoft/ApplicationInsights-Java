package com.microsoft.applicationinsights.telemetry;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.schemav2.PageViewData;

/**
 * Created by gupele on 1/13/2015.
 */
public final class PageViewTelemetry extends BaseTelemetry<PageViewData> {
    private PageViewData data;
    private ConcurrentMap<String, Double> metrics;

    public PageViewTelemetry() {
        this.data = new PageViewData();
        initialize(this.data.getProperties());
    }

    public PageViewTelemetry(String pageName) {
        this();
        this.data.setName(pageName);
    }

    public URI getUri() {
        URI result = Sanitizer.safeStringToUri(data.getUrl());
        if (result == null) {
            data.setUrl(null);
        }

        return result;
    }

    public void setUrl(URL url) {
        data.setUrl(url == null ? null : url.getPath());
    }

    public long getDuration() {
        return data.getDuration();
    }

    public void setDuration(long duration) {
        data.setDuration(duration);
    }

    public ConcurrentMap<String, Double> getMetrics() {
        return metrics;
    }

    public void setMetrics(ConcurrentMap<String, Double> metrics) {
        this.metrics = metrics;
    }

    @Override
    protected void additionalSanitize() {
        data.setName(Sanitizer.sanitizeName(this.data.getName()));
        Sanitizer.sanitizeMeasurements(this.getMetrics());
        Sanitizer.sanitizeUri(data.getUrl());
    }

    @Override
    protected PageViewData getData() {
        return data;
    }
}
