package com.microsoft.applicationinsights.internal.channel.samplingV2;

import com.microsoft.applicationinsights.agent.internal.common.StringUtils;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.Random;

/**
 * Created by Dhaval Doshi Oct 2017
 * This class generates the sample using the random number generator.
 * It also contains the logic to preserve the correlated telemetry items.
 */
public class SamplingScoreGeneratorV2 {

    private static Random random = new Random();

    /**
     * This method takes the telemetry and returns the hash of the operation id if it is present already
     * or uses the random number generator to generate the sampling score.
     * @param telemetry
     * @return
     */
    public static double getSamplingScore(Telemetry telemetry) {

        double samplingScore = 0.0;

        if (!StringUtils.isNullOrEmpty(telemetry.getContext().getOperation().getId())) {
            samplingScore =  ((double) getSamplingHashCode(telemetry.getContext().getOperation().getId()) / Integer.MAX_VALUE);
        }

        else {
            long val = Math.abs(random.nextLong());
            samplingScore =  ((double)Math.abs(val)/ Long.MAX_VALUE);
        }

        return samplingScore * 100;
    }

     static int getSamplingHashCode(String input) {
        if (StringUtils.isNullOrEmpty(input)) {
            return 0;
        }

        StringBuilder inputBuilder = new StringBuilder(input);
        while (inputBuilder.length() < 8) {
            inputBuilder.append(input);
        }

        int hash = 5381;

        for (int i = 0; i < inputBuilder.length(); ++i) {
            hash = ((hash << 5) + hash) + (int) inputBuilder.charAt(i);
        }

        return hash == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(hash);
    }
}
