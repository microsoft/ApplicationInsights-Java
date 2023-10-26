package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class OtlpTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.builder()
      .useOtlpEndpoint()
      .setSelfDiagnosticsLevel("DEBUG")
      .build();

//  @Autowired TestRestTemplate template;

  @Test
  @TargetUri("/ping")
  public void testOtlpTelemetry() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /OTLP/");

//    template.getForObject(URI.create("http://localhost:4317/ping"), String.class);

//    Thread.sleep(10000);

//    testing.mockedOtlpIngestion.verify();
  }

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends OtlpTest {}
}

