package com.microsoft.applicationinsights.agentc.internal.diagnostics.log;

import ch.qos.logback.contrib.json.JsonFormatter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Moshi.Builder;

import java.util.Map;

public class MoshiJsonFormatter implements JsonFormatter {
    /**
     * Used in testing, not in prod.
     */
    private boolean prettyPrint;

    @Override
    public String toJsonString(Map m) throws Exception {
        final Moshi moshi = new Builder().build();
        final JsonAdapter<Map> adapter;
        if (prettyPrint) {
            adapter = moshi.adapter(Map.class).indent("  ");
        } else {
            adapter = moshi.adapter(Map.class);
        }
        return adapter.toJson(m);
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
}
