/*
 * ApplicationInsights-Java
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

package com.microsoft.applicationinsights.web.spring;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

/**
 * Created by moralt on 4/30/2015.
 */
@XmlRootElement(name = "IntegrationTestConfiguration")
public class IntegrationTestConfiguration {

    private String storageConnectionString;
    private String queueName;
    private String testServerAddress;
    private String applicationFolder;
    private int port;
    private int pollingInterval;
    private int secondsToSleep;
    private int numberOfMessagesToRetrieve;

    private IntegrationTestConfiguration() {}

    public static IntegrationTestConfiguration load(String filename) throws JAXBException {
        System.out.println("Loading configuration from " + filename);

        JAXBContext jaxbContext = JAXBContext.newInstance(IntegrationTestConfiguration.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        File configurationFile = new File(filename);
        IntegrationTestConfiguration config = (IntegrationTestConfiguration)unmarshaller.unmarshal(configurationFile);

        System.out.println("Configuration file loaded successfully");

        return config;
    }

    public String getStorageConnectionString() {
        return storageConnectionString;
    }

    @XmlElement(name = "storageConnectionString")
    public void setStorageConnectionString(String storageConnectionString) {
        this.storageConnectionString = storageConnectionString;
    }

    public String getQueueName() {
        return queueName;
    }

    @XmlElement(name = "queueName")
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getTestServerAddress() {
        return testServerAddress;
    }

    @XmlElement(name = "testServerAddress")
    public void setTestServerAddress(String testServerAddress) {
        this.testServerAddress = testServerAddress;
    }

    public String getApplicationFolder() {
        return applicationFolder;
    }

    @XmlElement(name = "applicationFolder")
    public void setApplicationFolder(String applicationFolder) {
        this.applicationFolder = applicationFolder;
    }

    public int getPort() {
        return port;
    }

    @XmlElement(name = "port")
    public void setPort(int port) {
        this.port = port;
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    @XmlElement(name = "pollingInterval")
    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public int getSecondsToSleep() {
        return secondsToSleep;
    }

    @XmlElement(name = "secondsToSleep")
    public void setSecondsToSleep(int secondsToSleep) {
        this.secondsToSleep = secondsToSleep;
    }

    public int getNumberOfMessagesToRetrieve() {
        return numberOfMessagesToRetrieve;
    }

    @XmlElement(name = "numberOfMessagesToRetrieve")
    public void setNumberOfMessagesToRetrieve(int numberOfMessagesToRetrieve) {
        this.numberOfMessagesToRetrieve = numberOfMessagesToRetrieve;
    }
}
