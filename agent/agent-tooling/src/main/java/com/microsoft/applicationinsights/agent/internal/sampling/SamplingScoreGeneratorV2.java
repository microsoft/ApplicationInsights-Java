package com.microsoft.applicationinsights.agent.internal.sampling;

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
     * @param operationId
     * @return [0.0, 1.0)
     */
    public static double getSamplingScore(String operationId) {

        double samplingScore;

        if (!StringUtils.isEmpty(operationId)) {
            samplingScore =  ((double) getSamplingHashCode(operationId) / Integer.MAX_VALUE);
        } else {
            samplingScore =  random.nextDouble(); // [0,1)
        }

        return samplingScore * 100.0; // always < 100.0
    }

    /**
     * @param operationId
     * @return [0, Integer.MAX_VALUE)
     */
    static int getSamplingHashCode(String operationId) {
        if (StringUtils.isEmpty(operationId)) {
            return 0;
        }

        CharSequence opId;
        if (operationId.length() < 8) {
            StringBuilder opIdBuilder = new StringBuilder(operationId);
            while (opIdBuilder.length() < 8) {
                opIdBuilder.append(operationId);
            }
            opId = opIdBuilder;
        } else {
            opId = operationId;
        }

        int hash = 5381;

        for (int i = 0; i < opId.length(); ++i) {
            hash = ((hash << 5) + hash) + (int) opId.charAt(i);
        }

        if (hash == Integer.MIN_VALUE || hash == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE - 1;
        }
        return Math.abs(hash);
    }
}
