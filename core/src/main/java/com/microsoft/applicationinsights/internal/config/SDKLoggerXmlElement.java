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

package com.microsoft.applicationinsights.internal.config;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
/**
 * Created by gupele on 3/15/2015.
 */
@XmlRootElement(name="SDKLogger")
public class SDKLoggerXmlElement {
    private String type = "CONSOLE";
    private String level;
    private String uniquePrefix;
    private String baseFolder;
    private String numberOfFiles;
    private String numberOfTotalSizeInMB;

    public String getType() {
        return type;
    }

    @XmlAttribute(name="type")
    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    @XmlElement(name="Level")
    public void setLevel(String level) {
        this.level = level;
    }

    public String getUniquePrefix() {
        return uniquePrefix;
    }

    @XmlElement(name="UniquePrefix")
    public void setUniquePrefix(String uniquePrefix) {
        this.uniquePrefix = uniquePrefix;
    }

    public String getBaseFolder() {
        return baseFolder;
    }

    @XmlElement(name="BaseFolder")
    public void setBaseFolder(String baseFolder) {
        this.baseFolder = baseFolder;
    }

    public String getNumberOfFiles() {
        return numberOfFiles;
    }

    @XmlElement(name="NumberOfFiles")
    public void setNumberOfFiles(String numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }

    public String getNumberOfTotalSizeInMB() {
        return numberOfTotalSizeInMB;
    }

    @XmlElement(name="NumberOfTotalSizeInMB")
    public void setNumberOfTotalSizeInMB(String numberOfTotalSizeInMB) {
        this.numberOfTotalSizeInMB = numberOfTotalSizeInMB;
    }

    public Map<String, String> getData() {
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("Level", getLevel());
        data.put("UniquePrefix", getUniquePrefix());
        data.put("BaseFolder", getBaseFolder());
        data.put("NumberOfFiles", getNumberOfFiles());
        data.put("NumberOfTotalSizeInMB", getNumberOfTotalSizeInMB());

        return data;
    }
}
