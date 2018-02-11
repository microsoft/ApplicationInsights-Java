package com.microsoft.applicationinsights.channel.concrete.inprocess;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.http.Header;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandler;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicy;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;


public class ThrottlingHandler implements TransmissionHandler {

	private TransmissionPolicyManager transmissionPolicyManager;
	private final static String RESPONSE_RETRY_AFTER_DATE_FORMAT = "E, dd MMM yyyy HH:mm:ss";
	
	public ThrottlingHandler(TransmissionPolicyManager policy)
	{
		this.transmissionPolicyManager = policy;
	}
	
	@Override
	public void onTransmissionSent(TransmissionHandlerArgs args) {
		if (args.getRetryHeader() != null && args.getTransmission() != null && args.getTransmissionDispatcher() != null)
		{
			args.getTransmission().incrementNumberOfSends();
			switch (args.getResponseCode())
			{
			case 429:
			case 439:
				suspendTransmissions(TransmissionPolicy.BLOCKED_BUT_CAN_BE_PERSISTED, args.getRetryHeader());
				args.getTransmissionDispatcher().dispatch(args.getTransmission());
				break;
			}        
		}
	}
	
	
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
				InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR,
						"Throttled but failed to block transmission, exception: %s", e.getMessage());
			}
		
	}

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
