package com.microsoft.applicationinsights.internal.channel;


/**
 * Enables the {@link com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager} to handle transmission states.
 * <p>
 * This interface extends {@link TransmissionHandler} to add the ability to observe when the transmission is completed.
 * @author jamdavi
 *
 */
public interface TransmissionHandlerObserver extends TransmissionHandler {
	
	/**
	 * Used to add a {@link TransmissionHandler} to the collection stored by the 
     * {@link com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager}.
	 * @param handler	The handler to add to the collection.
	 */
	void addTransmissionHandler(TransmissionHandler handler);
}
