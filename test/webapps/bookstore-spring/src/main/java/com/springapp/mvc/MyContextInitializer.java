package com.springapp.mvc;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;

public class MyContextInitializer implements ContextInitializer {
	private String userName;

	public MyContextInitializer() {
		InputStream input = null;
		try {
			Properties prop = new Properties();
	    	input = MyContextInitializer.class.getClassLoader().getResourceAsStream("config.properties");
	    	
	    	if (input != null) {
	    		prop.load(input);
	    		
	    		userName = prop.getProperty("user.name");
	    	}
		} catch (IOException ex) {
    		ex.printStackTrace();
        } finally{
        	if(input!=null){
        		try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        	}
        }
	}
	
	public void initialize(TelemetryContext context) {
		if (userName != null) {
			context.getProperties().put("UserName", userName);
		}
	}
}