package com.microsoft.applicationinsights.agent.bootstrap.configuration;

import com.squareup.moshi.Moshi;

public class MoshiBuilderFactory {
    public static Moshi createBasicBuilder() {
        return new Moshi.Builder().build();
    }

    public static Moshi createBuilderWithAdaptor() {
        return new Moshi.Builder()
                .add(new ProcessorActionAdaptor())
                .build();
    }
}
