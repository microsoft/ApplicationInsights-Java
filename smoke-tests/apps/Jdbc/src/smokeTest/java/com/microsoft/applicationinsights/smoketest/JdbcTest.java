/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

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
public class JdbcTest extends AiWarSmokeTest {

  @Test
  @TargetUri("/hsqldbPreparedStatement")
  public void hsqldbPreparedStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT testdb.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertEquals("testdb", telemetry.rdd1.getTarget());
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/hsqldbStatement")
  public void hsqldbStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT testdb.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertEquals("testdb", telemetry.rdd1.getTarget());
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/hsqldbLargeStatement")
  public void hsqldbLargeStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    StringBuilder a2000 = new StringBuilder();
    for (int i = 0; i < 2000; i++) {
      a2000.append("a");
    }
    String largeStr = " /*" + a2000 + "*/";
    String query = "select * from abc" + largeStr;
    String truncatedQuery = query.substring(0, Math.min(query.length(), 1024));

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT testdb.abc");
    assertEquals(query, telemetry.rdd1.getData());
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertEquals("testdb", telemetry.rdd1.getTarget());
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/hsqldbBatchPreparedStatement")
  public void hsqldbBatchPreparedStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("INSERT testdb.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("insert into abc (xyz) values (?)");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertEquals("testdb", telemetry.rdd1.getTarget());
    // assertEquals(" [Batch of 3]", telemetry.rdd1.getProperties().get("Args"));
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Ignore // OpenTelemetry auto-instrumentation does not support non- prepared statement batching
  // yet
  @Test
  @TargetUri("/hsqldbBatchStatement")
  public void hsqldbBatchStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("insert testdb.abc");
    assertEquals(
        "insert into abc (xyz) values ('t'); insert into abc (xyz) values ('u');"
            + " insert into abc (xyz) values ('v')",
        telemetry.rdd1.getData());
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertEquals("testdb", telemetry.rdd1.getTarget());
    assertEquals(1, telemetry.rdd1.getProperties().size());
    assertEquals(" [Batch]", telemetry.rdd1.getProperties().get("Args"));
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/mysqlPreparedStatement")
  public void mysqlPreparedStatement() throws Exception {
    // exclude internal queries
    Telemetry telemetry =
        getTelemetry(1, rdd -> !rdd.getData().startsWith("/* mysql-connector-java? "));

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT mysql.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("mysql");
    // not the best test, because this is both the db.name and db.system
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+ \\| mysql");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/mysqlStatement")
  public void mysqlStatement() throws Exception {
    // exclude internal queries
    Telemetry telemetry =
        getTelemetry(1, rdd -> !rdd.getData().startsWith("/* mysql-connector-java? "));

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT mysql.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc");
    assertThat(telemetry.rdd1.getType()).isEqualTo("mysql");
    // not the best test, because this is both the db.name and db.system
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+ \\| mysql");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/postgresPreparedStatement")
  public void postgresPreparedStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT postgres.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("postgresql");
    // not the best test, because this is both the db.name and db.system
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+ \\| postgres");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/postgresStatement")
  public void postgresStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT postgres.abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc");
    assertThat(telemetry.rdd1.getType()).isEqualTo("postgresql");
    // not the best test, because this is both the db.name and db.system
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+ \\| postgres");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/sqlServerPreparedStatement")
  public void sqlServerPreparedStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Test
  @TargetUri("/sqlServerStatement")
  public void sqlServerStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Ignore("FIXME: need custom container with oracle db")
  @Test
  @TargetUri("/oraclePreparedStatement")
  public void oraclePreparedStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc where xyz = ?");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }

  @Ignore("FIXME: need custom container with oracle db")
  @Test
  @TargetUri("/oracleStatement")
  public void oracleStatement() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("SELECT abc");
    assertThat(telemetry.rdd1.getData()).isEqualTo("select * from abc");
    assertThat(telemetry.rdd1.getType()).isEqualTo("SQL");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jdbc/*");
  }
}
