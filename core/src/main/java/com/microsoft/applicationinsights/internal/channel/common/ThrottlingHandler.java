package com.microsoft.applicationinsights.internal.channel.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.http.Header;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandler;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the retry logic for throttled requests. HTTP status
 * code 429 and 439.
 *
 * @author jamdavi
 *
 */
public class ThrottlingHandler implements TransmissionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ThrottlingHandler.class);

    private TransmissionPolicyManager transmissionPolicyManager;
    private final static String RESPONSE_RETRY_AFTER_DATE_FORMAT = "E, dd MMM yyyy HH:mm:ss";

    /**
     * Ctor
     *
     * Constructs the ThrottlingHandler object.
     *
     * @param policy
     *            The {@link TransmissionPolicyManager} object that is needed to
     *            control the back off policy.
     */
    public ThrottlingHandler(TransmissionPolicyManager policy) {
        this.transmissionPolicyManager = policy;
    }

    @Override
    public void onTransmissionSent(TransmissionHandlerArgs args) {
        validateTransmissionAndSend(args);
    }

    /**
     * Provides the core logic for the retransmission
     *
     * @param args
     *            The {@link TransmissionHandlerArgs} for this transmission.
     * @return Returns a pass/fail for handling this transmission.
     */
    boolean validateTransmissionAndSend(TransmissionHandlerArgs args) {
        if (args.getRetryHeader() != null && args.getTransmission() != null
                && args.getTransmissionDispatcher() != null) {
            args.getTransmission().incrementNumberOfSends();
            switch (args.getResponseCode()) {
            case TransmissionSendResult.THROTTLED:
            case TransmissionSendResult.THROTTLED_OVER_EXTENDED_TIME:
                suspendTransmissions(TransmissionPolicy.BLOCKED_BUT_CAN_BE_PERSISTED, args.getRetryHeader());
                args.getTransmissionDispatcher().dispatch(args.getTransmission());
                return true;
            default:
                logger.trace("Http response code {} not handled by {}", args.getResponseCode(),
                        this.getClass().getName());
                return false;
            }
        }
        logger.trace("Http response code {} not handled by {}.", args.getResponseCode(),
                this.getClass().getName());
        return false;
    }

    /**
     * Used to put the sender thread to sleep for the specified duration in the
     * Retry-After header.
     *
     * @param suspensionPolicy
     *            The policy used to suspend the threads. For now we use
     *            {@link TransmissionPolicy.BLOCKED_BUT_CAN_BE_PERSISTED} for reuse
     *            of the existing logic.
     * @param retryAfterHeader
     *            The header that is captured from the HTTP response.
     */
    private void suspendTransmissions(TransmissionPolicy suspensionPolicy, Header retryAfterHeader) {

        if (retryAfterHeader == null) {
            return;
        }
        String retryAfterAsString = retryAfterHeader.getValue();
        if (Strings.isNullOrEmpty(retryAfterAsString)) {
            return;
        }

        try {
            DateFormat formatter = new SimpleDateFormat(RESPONSE_RETRY_AFTER_DATE_FORMAT);
            Date date = formatter.parse(retryAfterAsString);

            Date now = Calendar.getInstance().getTime();
            long retryAfterAsSeconds = (date.getTime() - convertToDateToGmt(now).getTime()) / 1000;
            this.transmissionPolicyManager.suspendInSeconds(suspensionPolicy, retryAfterAsSeconds);
        } catch (Throwable e) {
            logger.error("Throttled but failed to block transmission", e);
            this.transmissionPolicyManager.backoff();
        }

    }

    /**
     * Converts parsed date value to GMT for the {@link suspendTransmissions}
     * method.
     *
     * @param date
     *            The date to convert to GMT
     * @return The corrected date.
     */
    private static Date convertToDateToGmt(Date date) {
        TimeZone tz = TimeZone.getDefault();
        Date ret = new Date(date.getTime() - tz.getRawOffset());

        // If we are now in DST, back off by the delta. Note that we are checking the
        // GMT date, this is the KEY.
        if (tz.inDaylightTime(ret)) {
            Date dstDate = new Date(ret.getTime() - tz.getDSTSavings());

            // Check to make sure we have not crossed back into standard time
            if (tz.inDaylightTime(dstDate)) {
                ret = dstDate;
            }
        }
        return ret;
    }
}
