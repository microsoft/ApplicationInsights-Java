package com.microsoft.applicationinsights.azuretest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

/**
 * Created by moralt on 30/4/2015.
 */
@XmlRootElement(name = "IntegrationTestConfiguration")
public class IntegrationTestConfiguration {

    private String storageConnectionString;
    private String queueName;
    private String userAgent;
    private String testServerAddress;
    private String applicationFolder;
    private int tomcat7Port;
    private int jetty8Port;
    private int secondsToPoll;
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

    public String getUserAgent() {
        return userAgent;
    }

    @XmlElement(name = "userAgent")
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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

    public int getTomcat7Port() {
        return tomcat7Port;
    }

    @XmlElement(name = "tomcat7Port")
    public void setTomcat7Port(int tomcat7Port) {
        this.tomcat7Port = tomcat7Port;
    }

    public int getJetty8Port() {
        return jetty8Port;
    }

    @XmlElement(name = "jetty8Port")
    public void setJetty8Port(int jetty8Port) {
        this.jetty8Port = jetty8Port;
    }

    public int getSecondsToPoll() {
        return secondsToPoll;
    }

    @XmlElement(name = "secondsToPoll")
    public void setSecondsToPoll(int secondsToPoll) {
        this.secondsToPoll = secondsToPoll;
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
