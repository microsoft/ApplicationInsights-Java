package com.microsoft.applicationinsights.channel;

import java.util.concurrent.TimeUnit;

/**
 * Created by gupele on 12/22/2014.
 */
public interface TransmissionsLoader {
    boolean load(boolean waitForThreadsToStart);

    void stop(long timeout, TimeUnit timeUnit);
}
