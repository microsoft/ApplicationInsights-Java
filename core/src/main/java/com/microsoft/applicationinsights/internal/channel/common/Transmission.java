/*
 * AppInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.channel.common;

import java.io.Serializable;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Holds the stuff that defines a transmission of data to the server.
 * It also holds the meta data that describes the content, for example encoding type
 *
 * Created by gupele on 12/17/2014.
 */
public final class Transmission implements Serializable {
    private int version;

    private int numberOfSends;

    private int numberOfPersistence;

    private final byte[] content;

    private final String webContentType;

    private final String webContentEncodingType;

    public Transmission(byte[] content, String webContentType, String webContentEncodingType, int version) {
        Preconditions.checkNotNull(content, "Content must be non-null value");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(webContentType), "webContentType must be a non empty string");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(webContentEncodingType), "webContentEncodingType must be a non empty string");

        numberOfSends = numberOfPersistence = 0;
        this.version = version;
        this.content = content;
        this.webContentType = webContentType;
        this.webContentEncodingType = webContentEncodingType;
    }

    public Transmission(byte[] content, String webContentType, String webContentEncodingType) {
        this(content, webContentType, webContentEncodingType, 1);
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

    public void incrementNumberOfSends() {
        ++numberOfSends;
    }

    public void incrementNumberOfPersistence() {
        ++numberOfPersistence;
    }

    public int getNumberOfSends() {
        return numberOfSends;
    }

    public void setNumberOfSends(int numberOfSends) {
        this.numberOfSends = numberOfSends;
    }

    public int getNumberOfPersistence() {
        return numberOfPersistence;
    }

    public void setNumberOfPersistence(int numberOfPersistence) {
        this.numberOfPersistence = numberOfPersistence;
    }

    public int getVersion() {
        return version;
    }
}
