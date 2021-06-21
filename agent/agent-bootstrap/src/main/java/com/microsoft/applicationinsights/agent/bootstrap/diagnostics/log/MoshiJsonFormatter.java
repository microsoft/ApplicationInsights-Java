package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import java.util.Map;

import ch.qos.logback.contrib.json.JsonFormatter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Moshi.Builder;

public class MoshiJsonFormatter implements JsonFormatter {

    // only used in tests
    private boolean prettyPrint;

    @Override
    public String toJsonString(Map m) {
        Moshi moshi = new Builder().build();
        if (prettyPrint) {
            return moshi.adapter(Map.class).indent("  ").toJson(m);
        } else {
            return moshi.adapter(Map.class).toJson(m);
        }
    }

    // only used in tests
    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
}
