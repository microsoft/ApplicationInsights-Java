package AzureTest;

public interface ITelemetryItem {
	/**
	 * Gets the document type of the telemetry item
	 * @return The document type of the telemetry item
	 */
	public DocumentType getDocType();

	/**
	 * Gets a property of the telemetry item
	 * @param name The property name
	 * @return The property of the telemetry item
	 */
	public String getProperty(String name);

	/**
	 * Sets a property of the telemetry item
	 * @param name The property name
	 * @param value The property value
	 */
	public void setProperty(String name, String value);

	/**
	 * Tests if the properties of the this item equals to the properties of another telemetry item
	 * @param telemetry The other telemetry item
	 * @return True if equals, otherwise false.
	 */
	public boolean equalsByProperties(ITelemetryItem telemetry);
}
