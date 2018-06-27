package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

@UseAgent
@WithDependencyContainers(@DependencyContainer(value = "redis", portMapping = "6379"))
public class SampleTestWithDependencyContainer extends AiSmokeTest {

  // TODO FIXME this is a sample test for dependencies. it shouldn't count towards test coverage
  @Test
  @TargetUri("/index.jsp")
  public void doCalcSendsRequestDataAndMetricData() throws Exception {
    assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
    assertTrue("mocked ingestion has 0 items", mockedIngestion.getItemCount() > 0);

    assertEquals(1, mockedIngestion.getCountForType("RequestData"));
    assertEquals(2, mockedIngestion.getCountForType("RemoteDependencyData"));
  }
}
