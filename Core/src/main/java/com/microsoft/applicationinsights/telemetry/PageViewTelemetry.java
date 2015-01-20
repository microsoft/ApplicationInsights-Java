package com.microsoft.applicationinsights.telemetry;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.schemav2.PageViewData;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * Telemetry type used to track page views.
 *
 * You can send information about pages viewed by your application to Application Insights by
 * passing an instance of this class to the 'trackPageView' method of the {@link com.microsoft.applicationinsights.TelemetryClient}
 */
public final class PageViewTelemetry extends BaseTelemetry<PageViewData> {
    private PageViewData data;

    /**
     * Default Ctor
     */
    public PageViewTelemetry() {
        data = new PageViewData();
        initialize(data.getProperties());
    }

    /**
     * Initializes a new instance of the class with the specified 'pageName'
     * @param pageName The name of page to track.
     */
    public PageViewTelemetry(String pageName) {
        this();
        setName(pageName);
    }

    /**
     * Sets the name of the page view.
     * @param name The page view name.
     */
    public void setName(String name) {
        data.setName(name);
    }

    /**
     * Gets the name of the page view.
     * @return The page view name.
     */
    public String getName() {
        return data.getName();
    }

    /**
     * Gets the page view Uri.
     * @return The page view Uri.
     */
    public URI getUri() {
        URI result = Sanitizer.safeStringToUri(data.getUrl());
        if (result == null) {
            data.setUrl(null);
        }

        return result;
    }

    /**
     * Sets the page view Uri.
     * @param url The page view Uri.
     */
    public void setUrl(URI url) {
        data.setUrl(url == null ? null : url.toString());
    }

    /**
     * Gets the page view duration.
     * @return The page view duration.
     */
    public long getDuration() {
        return data.getDuration();
    }

    /**
     * Sets the page view duration.
     * @param duration The page view duration.
     */
    public void setDuration(long duration) {
        data.setDuration(duration);
    }

    /**
     * Gets a dictionary of custom defined metrics.
     * @return Custom defined metrics.
     */
    public ConcurrentMap<String, Double> getMetrics() {
        return data.getMeasurements();
    }

    @Override
    protected void additionalSanitize() {
        data.setName(Sanitizer.sanitizeName(data.getName()));
        Sanitizer.sanitizeMeasurements(this.getMetrics());
        Sanitizer.sanitizeUri(data.getUrl());
    }

    @Override
    protected PageViewData getData() {
        return data;
    }
}
