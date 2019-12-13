package com.microsoft.applicationinsights.internal.channel.samplingV2;

import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.Random;
import org.apache.commons.lang3.StringUtils;

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
     * @return [0.0, 1.0)
     */
    public static double getSamplingScore(Telemetry telemetry) {

        double samplingScore;

        if (!StringUtils.isEmpty(telemetry.getContext().getOperation().getId())) {
            samplingScore =  ((double) getSamplingHashCode(telemetry.getContext().getOperation().getId()) / Integer.MAX_VALUE);
        } else {
            samplingScore =  random.nextDouble(); // [0,1)
        }

        return samplingScore * 100.0; // always < 100.0
    }

    /**
     * @param input
     * @return [0, Integer.MAX_VALUE)
     */
    static int getSamplingHashCode(String input) {
        if (StringUtils.isEmpty(input)) {
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

        if (hash == Integer.MIN_VALUE || hash == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE - 1;
        }
        return Math.abs(hash);
    }
}
