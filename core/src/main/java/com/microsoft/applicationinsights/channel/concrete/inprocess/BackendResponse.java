package com.microsoft.applicationinsights.channel.concrete.inprocess;

public class BackendResponse {

	public int itemsReceived;
    public int itemsAccepted;
    public Error[] errors;

    class Error
    {       
        public int index;
        public int statusCode;      
        public String message;
    }
}
