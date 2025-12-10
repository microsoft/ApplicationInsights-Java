// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;

@Environment(TOMCAT_8_JAVA_8)
@UseAgent("capture_params_applicationinsights.json")
class JdbcCaptureParametersTest extends AbstractJdbcUnmasked {

  @Test
  @TargetUri("/hsqldbPreparedStatement")
  void hsqldbPreparedStatementCapturesParameters() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT testdb.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where uvw = ? and xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).isEqualTo("hsqldb | testdb");
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getProperties())
        .containsExactly(entry("db.query.parameter.0", "v"), entry("db.query.parameter.1", "y"));

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }
}
