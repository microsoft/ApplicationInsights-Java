package com.microsoft.applicationinsights.internal.channel.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandler;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

/**
 * This class implements the retry logic for throttled requests. HTTP status
 * code 429 and 439.
 * 
 * @author jamdavi
 *
 */
public class ThrottlingHandler implements TransmissionHandler {

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
			case 429:
			case 439:
				suspendTransmissions(TransmissionPolicy.BLOCKED_BUT_CAN_BE_PERSISTED, args.getRetryHeader());
				args.getTransmissionDispatcher().dispatch(args.getTransmission());
				return true;
			default:
				InternalLogger.INSTANCE.trace("Http response code %s not handled by %s", args.getResponseCode(),
						this.getClass().getName());
				return false;
			}
		}
		InternalLogger.INSTANCE.trace("Http response code %s not handled by %s.", args.getResponseCode(),
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
			InternalLogger.INSTANCE.error("Throttled but failed to block transmission.\r\n" + "Stack Trace:\r\n" + "%s",
					ExceptionUtils.getStackTrace(e));
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
