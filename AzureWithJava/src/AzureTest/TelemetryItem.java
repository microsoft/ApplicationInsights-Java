package AzureTest;

import java.util.*;

public class TelemetryItem implements ITelemetryItem {
	
	private static final Hashtable<DocumentType, ArrayList<String>> defaultPropertiesToCompare = new Hashtable<DocumentType, ArrayList<String>>() {{
		put(DocumentType.Requests, new ArrayList<String>() {{
			add("port"); 
			add("responseCode");
			add("uri");
			add("userAgent");
			// add("queryParameter..."); TODO logic to compare parameters with a general name.
		}});
		put(DocumentType.Requests, new ArrayList<String>() {{
			add("category"); 
			add("instance");
		}});
	}};
	
	private DocumentType docType;
	
	private Hashtable<String, String> properties = new Hashtable<String, String>();

	/**
	 * Initializes a new TelemetryItem object
	 * @param docType The document type of the telemetry item
	 */
	public TelemetryItem(DocumentType docType) {
		this.docType = docType;
	}
	
	public DocumentType getDocType() {
		return this.docType;
	}
	
	public String getProperty(String name) {
		return this.properties.get(name);
	}
	
	public void setProperty(String name, String value) {
		this.properties.put(name, value);
	}
	
	public boolean equalsByProperties(ITelemetryItem telemetry) {
		
		if(this == telemetry) {
			return true;
		}
		
		if(telemetry == null || telemetry.getDocType() != this.getDocType()) {
			return false;
		}
		
		for (String propertyName : defaultPropertiesToCompare.get(this.getDocType())) {
			if(telemetry.getProperty(propertyName) == null || this.getProperty(propertyName) == null) {
				return false;
			}
			
			if(!telemetry.getProperty(propertyName).equals(this.getProperty(propertyName))) {
				return false;
			}
		}
	
		return true;
	}
}
