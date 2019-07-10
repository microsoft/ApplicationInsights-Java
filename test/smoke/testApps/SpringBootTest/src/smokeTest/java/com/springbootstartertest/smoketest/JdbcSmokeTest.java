package com.springbootstartertest.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.*;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseAgent
@WithDependencyContainers({
        @DependencyContainer(
                value = "mysql:5",
                environmentVariables = {"MYSQL_ROOT_PASSWORD=password"},
                portMapping = "3306",
                hostnameEnvironmentVariable = "MYSQL"),
        @DependencyContainer(
                value = "postgres:11",
                portMapping = "5432",
                hostnameEnvironmentVariable = "POSTGRES"),
        @DependencyContainer(
                value = "mcr.microsoft.com/mssql/server:2017-latest",
                environmentVariables = {"ACCEPT_EULA=Y", "SA_PASSWORD=Password1"},
                portMapping = "1433",
                hostnameEnvironmentVariable = "SQLSERVER")
})
public class JdbcSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri("/jdbc/hsqldbPreparedStatement")
    public void hsqldbPreparedStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());
    }

    @Test
    @TargetUri("/jdbc/hsqldbStatement")
    public void hsqldbStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());
    }

    @Test
    @TargetUri("/jdbc/hsqldbBatchPreparedStatement")
    public void hsqldbBatchPreparedStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("insert into abc (xyz) values (?)", rdd.getData());
        assertEquals(" [Batch of 3]", rdd.getProperties().get("Args"));
        assertTrue(rdd.getSuccess());
    }

    @Test
    @TargetUri("/jdbc/hsqldbBatchStatement")
    public void hsqldbBatchStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("insert into abc (xyz) values ('t'); insert into abc (xyz) values ('u');" +
                " insert into abc (xyz) values ('v')", rdd.getData());
        assertEquals(" [Batch]", rdd.getProperties().get("Args"));
        assertTrue(rdd.getSuccess());
    }

    @Test
    @TargetUri("/jdbc/mysqlPreparedStatement")
    public void mysqlPreparedStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        // the old agent captured several internal queries, e.g. "SHOW WARNINGS"
        int total = mockedIngestion.getCountForType("RemoteDependencyData");
        assertTrue(total > 0);
        RemoteDependencyData rdd = null;
        boolean found = false;
        for (int i = 0; i < total; i++) {
            rdd = getTelemetryDataForType(i, "RemoteDependencyData");
            if (rdd.getData().equals("select * from abc where xyz = ?")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:mysql://"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());
    }

    @Test
    @TargetUri("/jdbc/mysqlStatement")
    public void mysqlStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        // the old agent captured several internal queries, e.g. "SHOW WARNINGS"
        int total = mockedIngestion.getCountForType("RemoteDependencyData");
        assertTrue(total > 0);
        RemoteDependencyData rdd = null;
        boolean found = false;
        for (int i = 0; i < total; i++) {
            rdd = getTelemetryDataForType(i, "RemoteDependencyData");
            if (rdd.getData().equals("select * from abc")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:mysql://"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());
    }

    @Test
    @TargetUri("/jdbc/postgresPreparedStatement")
    public void postgresPreparedStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:postgresql://"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());
    }

    @Test
    @TargetUri("/jdbc/postgresStatement")
    public void postgresStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:postgresql://"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());
    }

    @Test
    @TargetUri("/jdbc/sqlServerPreparedStatement")
    public void sqlServerPreparedStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:sqlserver://"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());
    }

    @Test
    @TargetUri("/jdbc/sqlServerStatement")
    public void sqlServerStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:sqlserver://"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());
    }

    @Ignore("FIXME: need custom container with oracle db")
    @Test
    @TargetUri("/jdbc/oraclePreparedStatement")
    public void oraclePreparedStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:oracle:thin:@"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());
    }

    @Ignore("FIXME: need custom container with oracle db")
    @Test
    @TargetUri("/jdbc/oracleStatement")
    public void oracleStatement() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:oracle:thin:@"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());
    }
}
