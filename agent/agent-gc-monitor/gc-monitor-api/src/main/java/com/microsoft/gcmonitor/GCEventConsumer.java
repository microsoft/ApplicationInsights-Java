package com.microsoft.gcmonitor;

import java.util.function.Consumer;

public interface GCEventConsumer extends Consumer<GCCollectionEvent> {
}
