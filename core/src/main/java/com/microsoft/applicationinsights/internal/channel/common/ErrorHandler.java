package com.microsoft.applicationinsights.internal.channel.common;

import com.microsoft.applicationinsights.internal.channel.TransmissionHandler;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the retry logic for transmissions with the results of a
 * 408, 500, and 503 result.
 * <p>
 * It does not handle any error codes such as 400, 401, 403, 404, etc.
 *
 * @author jamdavi
 *
 */
public class ErrorHandler implements TransmissionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    private TransmissionPolicyManager transmissionPolicyManager;

    /**
     * Ctor
     *
     * Constructs the ErrorHandler object.
     *
     * @param policy
     *            The {@link TransmissionPolicyManager} object that is needed to
     *            control the back off policy
     */
    public ErrorHandler(TransmissionPolicyManager policy) {
        this.transmissionPolicyManager = policy;
    }

    @Override
    public void onTransmissionSent(TransmissionHandlerArgs args) {

        validateTransmissionAndSend(args);
    }

    boolean validateTransmissionAndSend(TransmissionHandlerArgs args) {
        if (args.getTransmission() != null && args.getTransmissionDispatcher() != null) {
            args.getTransmission().incrementNumberOfSends();
            switch (args.getResponseCode()) {
            case TransmissionSendResult.REQUEST_TIMEOUT:
            case TransmissionSendResult.INTERNAL_SERVER_ERROR:
            case TransmissionSendResult.SERVICE_UNAVAILABLE:
                backoffAndSendTransmission(args);
                return true;
            default:
                logger.trace("Http response code {} not handled by {}", args.getResponseCode(),
                        this.getClass().getName());
                return false;
            }
        } else if (args.getException() != null) {
            backoffAndSendTransmission(args);
            return true;
        }
        return false;
    }

    private void backoffAndSendTransmission(TransmissionHandlerArgs args) {
        // It is possible for us to have a temporary blip in transmission
        // this setting will allow us to control how many instant retries we perform
        // before backing off the send
        if (args.getTransmission() != null && (args.getTransmission().getNumberOfSends() > transmissionPolicyManager.getMaxInstantRetries()))
        {
            this.transmissionPolicyManager.backoff();
        }
        args.getTransmissionDispatcher().dispatch(args.getTransmission());
    }
}
