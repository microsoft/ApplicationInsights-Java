// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
@WithDependencyContainers({
  @DependencyContainer(
      value = "mysql:5",
      environmentVariables = {"MYSQL_ROOT_PASSWORD=password"},
      exposedPort = 3306,
      hostnameEnvironmentVariable = "MYSQL"),
  @DependencyContainer(
      value = "postgres:11",
      exposedPort = 5432,
      environmentVariables = {"POSTGRES_PASSWORD=passw0rd2"},
      hostnameEnvironmentVariable = "POSTGRES"),
  @DependencyContainer(
      value = "mcr.microsoft.com/mssql/server:2019-latest",
      environmentVariables = {"ACCEPT_EULA=Y", "SA_PASSWORD=Password1"},
      exposedPort = 1433,
      hostnameEnvironmentVariable = "SQLSERVER")
})
abstract class JdbcTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/hsqldbPreparedStatement")
  void hsqldbPreparedStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT testdb.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).isEqualTo("hsqldb | testdb");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/hsqldbStatement")
  void hsqldbStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT testdb.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).isEqualTo("hsqldb | testdb");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/hsqldbLargeStatement")
  void hsqldbLargeStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    StringBuilder a2000 = new StringBuilder();
    for (int i = 0; i < 2000; i++) {
      a2000.append("a");
    }
    String largeStr = " /*" + a2000 + "*/";
    String query = "select * from abc" + largeStr;
    String truncatedQuery = query.substring(0, Math.min(query.length(), 1024));

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT testdb.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo(query);
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).isEqualTo("hsqldb | testdb");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/hsqldbBatchPreparedStatement")
  void hsqldbBatchPreparedStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("INSERT testdb.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("insert into abc (xyz) values (?)");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).isEqualTo("hsqldb | testdb");
    // assertThat(telemetry.rdd1.getProperties()).containsEntry("Args", " [Batch of 3]");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  // OpenTelemetry auto-instrumentation does not support non- prepared statement batching yet
  // @Test
  @TargetUri("/hsqldbBatchStatement")
  void hsqldbBatchStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("insert testdb.abc");
    assertThat(telemetry.rdd1.getData())
        .isEqualTo(
            "insert into abc (xyz) values ('t'); insert into abc (xyz) values ('u');"
                + " insert into abc (xyz) values ('v')");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).isEqualTo("testdb");
    assertThat(telemetry.rdd1.getProperties()).hasSize(1);
    assertThat(telemetry.rdd1.getProperties()).containsEntry("Args", " [Batch]");
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/mysqlPreparedStatement")
  void mysqlPreparedStatement() throws Exception {
    // exclude internal queries
    Telemetry telemetry =
        testing.getTelemetry(1, rdd -> !rdd.getData().startsWith("/* mysql-connector-java? "));

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT mysql.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("mysql");
    // not the best test, because this is both the db.name and db.system
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+ \\| mysql");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/mysqlStatement")
  void mysqlStatement() throws Exception {
    // exclude internal queries
    Telemetry telemetry =
        testing.getTelemetry(1, rdd -> !rdd.getData().startsWith("/* mysql-connector-java? "));

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT mysql.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("mysql");
    // not the best test, because this is both the db.name and db.system
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+ \\| mysql");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/postgresPreparedStatement")
  void postgresPreparedStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT postgres.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("postgresql");
    // not the best test, because this is both the db.name and db.system
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+ \\| postgres");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/postgresStatement")
  void postgresStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT postgres.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("postgresql");
    // not the best test, because this is both the db.name and db.system
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+ \\| postgres");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/sqlServerPreparedStatement")
  void sqlServerPreparedStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/sqlServerStatement")
  void sqlServerStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Disabled("need custom container with oracle db")
  @Test
  @TargetUri("/oraclePreparedStatement")
  void oraclePreparedStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Disabled("need custom container with oracle db")
  @Test
  @TargetUri("/oracleStatement")
  void oracleStatement() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends JdbcTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends JdbcTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends JdbcTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends JdbcTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends JdbcTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends JdbcTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends JdbcTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends JdbcTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends JdbcTest {}
}
