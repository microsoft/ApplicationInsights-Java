package com.microsoft.applicationinsights.channel;

import java.util.concurrent.TimeUnit;

/**
 * Created by gupele on 12/22/2014.
 */
public interface TransmissionsLoader {
    void load();

    void stop(long timeout, TimeUnit timeUnit);
}
