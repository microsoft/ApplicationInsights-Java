package com.microsoft.applicationinsights.channel;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Holds the stuff that defines a transmission of data to the server.
 * It also holds the meta data that describes the content, for example encoding type
 *
 * Created by gupele on 12/17/2014.
 */
public final class Transmission {
    private final byte[] content;

    private final String webContentType;

    private final String webContentEncodingType;

    public Transmission(byte[] content, String webContentType, String webContentEncodingType) {
        Preconditions.checkNotNull(content, "Content must be non-null value");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(webContentType), "webContentType must be a non empty string");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(webContentEncodingType), "webContentEncodingType must be a non empty string");

        this.content = content;
        this.webContentType = webContentType;
        this.webContentEncodingType = webContentEncodingType;
    }

    public byte[] getContent() {
        return content;
    }

    public String getWebContentType() {
        return webContentType;
    }

    public String getWebContentEncodingType() {
        return webContentEncodingType;
    }
}
