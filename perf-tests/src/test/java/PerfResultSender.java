// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

class PerfResultSender {

  private static final String TEST_NAME_PLACEHOLDER = "{TEST_NAME_PLACEHOLDER}";
  private static final String URL_ENCODED_DATE_PLACEHOLDER = "{URL_ENCODED_DATE_PLACEHOLDER}";
  private static final String METRIC_NAME_PLACEHOLDER = "{METRIC_NAME_PLACEHOLDER}";
  private static final String UNIT_OF_MEASURE_PLACEHOLDER = "{METRIC_UNIT_PLACEHOLDER}";
  private static final String METRIC_VALUE_PLACEHOLDER = "{METRIC_VALUE_PLACEHOLDER}";

  static void send(
      Date date, String testName, String metricName, String allocationValue, String unitOfMeasure)
      throws IOException {

    String urlEncodedDate = formatAndEncodeDate(date);

    String httpUrlWithTestValues =
        formatHttpUrlWithTestValues(
            urlEncodedDate, testName, metricName, allocationValue, unitOfMeasure);

    URL url = new URL(httpUrlWithTestValues);

    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

    connection.setRequestMethod("GET");

    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
      throw new IllegalStateException(
          "Error sending the perf result. Response code: "
              + responseCode
              + ". Encoded date: "
              + urlEncodedDate);
    }
  }

  private static String formatHttpUrlWithTestValues(
      String urlEncodedDate,
      String testName,
      String metricName,
      String metricValue,
      String metricUnit) {

    String perfTestUrlPattern = System.getenv("PERF_TEST_URL_PATTERN");
    if(perfTestUrlPattern == null || perfTestUrlPattern.isEmpty()) {
      throw new IllegalStateException("Unable to find the perf test url pattern");
    }

    return perfTestUrlPattern
        .replace(URL_ENCODED_DATE_PLACEHOLDER, urlEncodedDate)
        .replace(TEST_NAME_PLACEHOLDER, testName)
        .replace(UNIT_OF_MEASURE_PLACEHOLDER, metricUnit)
        .replace(METRIC_NAME_PLACEHOLDER, metricName)
        .replace(METRIC_VALUE_PLACEHOLDER, metricValue);
  }

  private static String formatAndEncodeDate(Date date) throws UnsupportedEncodingException {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
    String formattedDate = dateFormat.format(date);
    return URLEncoder.encode(formattedDate, StandardCharsets.UTF_8.toString());
  }
}
