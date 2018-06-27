package com.microsoft.applicationinsights.internal.channel.samplingV2;

import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

public class SamplingScoreGeneratorV2Test {

  private static Random random = new Random();

  private static String generateRandomUserId() {
    int max = 12;
    int min = 3;
    int userIdLength = getRandomInRange(min, max);
    StringBuffer userId = new StringBuffer();
    for (int i = 0; i < userIdLength; ++i) {
      userId.append((char) ('a' + getRandomInRange(0, 25)));
    }
    return userId.toString();
  }

  private static String generateRandomOperationId() {
    return String.valueOf(random.nextLong());
  }

  private static int getRandomInRange(int min, int max) {
    return random.nextInt((max - min) + 1) + min;
  }

  @Test
  public void samplingScoreGeneratedUsingOperationIdIfPresent() {

    String operationId = generateRandomOperationId();

    Telemetry eventTelemetry = new EventTelemetry();
    eventTelemetry.getContext().getOperation().setId(operationId);

    Telemetry requestTelemetry = new RequestTelemetry();
    requestTelemetry.getContext().getOperation().setId(operationId);

    double eventTelemetrySamplingScore = SamplingScoreGeneratorV2.getSamplingScore(eventTelemetry);
    double requestTelemetrySamplingScore =
        SamplingScoreGeneratorV2.getSamplingScore(requestTelemetry);

    Assert.assertEquals(eventTelemetrySamplingScore, requestTelemetrySamplingScore, 0.0);
  }

  @Test
  public void samplingScoreIsNotAffectedByPresenceOfUserId() {

    String userId1 = generateRandomUserId();
    String userId2 = generateRandomUserId();
    String operationId = generateRandomOperationId();

    Telemetry eventTelemetry = new EventTelemetry();
    eventTelemetry.getContext().getUser().setId(userId1);
    eventTelemetry.getContext().getOperation().setId(operationId);

    Telemetry requestTelemetry = new RequestTelemetry();
    requestTelemetry.getContext().getUser().setId(userId2);
    requestTelemetry.getContext().getOperation().setId(operationId);

    double eventTelemetrySamplingScore = SamplingScoreGeneratorV2.getSamplingScore(eventTelemetry);
    double requestTelemetrySamplingScore =
        SamplingScoreGeneratorV2.getSamplingScore(requestTelemetry);

    Assert.assertEquals(eventTelemetrySamplingScore, requestTelemetrySamplingScore, 0.0);
  }

  @Test
  public void samplingScoreIsRandomIfOperationIdIsNotPresent() {

    Telemetry eventTelemetry = new EventTelemetry();
    Telemetry requestTelemetry = new RequestTelemetry();
    double eventTelemetrySamplingScore = SamplingScoreGeneratorV2.getSamplingScore(eventTelemetry);
    double requestTelemetrySamplingScore =
        SamplingScoreGeneratorV2.getSamplingScore(requestTelemetry);
    Assert.assertNotEquals(eventTelemetrySamplingScore, requestTelemetrySamplingScore, 0.0);
  }

  @Test
  public void samplingScoreIsRandomIfUserIdIsPresentWithoutOperationId() {

    String userId = generateRandomUserId();
    Telemetry eventTelemetry = new EventTelemetry();
    eventTelemetry.getContext().getUser().setId(userId);

    Telemetry requestTelemetry = new RequestTelemetry();
    requestTelemetry.getContext().getUser().setId(userId);

    double eventTelemetrySamplingScore = SamplingScoreGeneratorV2.getSamplingScore(eventTelemetry);
    double requestTelemetrySamplingScore =
        SamplingScoreGeneratorV2.getSamplingScore(requestTelemetry);
    Assert.assertNotEquals(eventTelemetrySamplingScore, requestTelemetrySamplingScore, 0.0);
  }

  @Test
  public void stringSamplingHashCodeProducesConsistentValues() {

    // we have a predefined set of strings and their hash values
    // the test allows us to make sure we can produce the same hashing
    // results in different versions of sdk

    Map<String, Integer> stringHashMap =
        new HashMap<String, Integer>() {
          {
            put("ss", 1179811869);
            put("kxi", 168993463);
            put("wr", 1281077591);
            put("ynehgfhyuiltaiqovbpyhpm", 2139623659);
            put("iaxxtklcw", 1941943012);
            put("hjwvqjiiwhoxrtsjma", 1824011880);
            put("rpiauyg", 251412007);
            put("jekvjvh", 9189387);
            put("hq", 1807146729);
            put("kgqxrftjhefkwlufcxibwjcy", 270215819);
            put("lkfc", 1228617029);
            put("skrnpybqqu", 223230949);
            put("px", 70671963);
            put("dtn", 2050473033);
            put("nqfcxobaequ", 397313566);
            put("togxlt", 948170633);
            put("jvvdkhnahkaujxarkd", 1486894898);
            put("mcloukvkamiaqja", 56804453);
            put("ornuu", 1588005865);
            put("otodvlhtvu", 1544494884);
            put("uhpwhasnvmnykjkitla", 981289895);
            put("itbnryqnjcgpmjemdghqtg", 1469591400);
            put("wauetkdnivwlafbfhiedsfx", 2114415420);
            put("fniwmeidbvd", 508699380);
            put("vuwdgoxspstvj", 1821547235);
            put("y", 1406544563);
            put("pceqcixfb", 1282453766);
            put("aentke", 255756533);
            put("ni", 1696510239);
            put("lbwehevltlnl", 1466602040);
            put("ymxql", 1974582171);
            put("mvqbaosfuip", 1560556398);
            put("urmwofajwmmlornynglm", 701710403);
            put("buptyvonyacerrt", 1315240646);
            put("cxsqcnyieliatqnwc", 76148095);
            put("svvco", 1849105799);
            put("luwmjhwyt", 553630912);
            put("lisvmmug", 822987687);
            put("mmntilfbmxwuyij", 882214597);
            put("hqmyv", 1510970959);
          }
        };

    for (String key : stringHashMap.keySet()) {
      int calculatedHash = SamplingScoreGeneratorV2.getSamplingHashCode(key);
      Assert.assertTrue(stringHashMap.get(key) == calculatedHash);
    }
  }
}
