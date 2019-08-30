package com.microsoft.applicationinsights.smoketestapp;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.DependencyContainer;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import com.microsoft.applicationinsights.smoketest.WithDependencyContainers;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    @TargetUri("/hsqldbPreparedStatement")
    public void hsqldbPreparedStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/hsqldbStatement")
    public void hsqldbStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/hsqldbBatchPreparedStatement")
    public void hsqldbBatchPreparedStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("insert into abc (xyz) values (?)", rdd.getData());
        assertEquals(" [Batch of 3]", rdd.getProperties().get("Args"));
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/hsqldbBatchStatement")
    public void hsqldbBatchStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("insert into abc (xyz) values ('t'); insert into abc (xyz) values ('u');" +
                " insert into abc (xyz) values ('v')", rdd.getData());
        assertEquals(" [Batch]", rdd.getProperties().get("Args"));
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/mysqlPreparedStatement")
    public void mysqlPreparedStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        Envelope rddEnvelope = mockedIngestion.waitForItem(new Predicate<Envelope>() {
            @Override
            public boolean apply(Envelope input) {
                if (!input.getData().getBaseType().equals("RemoteDependencyData")) {
                    return false;
                }
                RemoteDependencyData rdd = (RemoteDependencyData) ((Data) input.getData()).getBaseData();
                // the old agent captured several internal queries, e.g. "SHOW WARNINGS"
                return rdd.getData().equals("select * from abc where xyz = ?");
            }
        }, 10, TimeUnit.SECONDS);


        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:mysql://"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/mysqlStatement")
    public void mysqlStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        Envelope rddEnvelope = mockedIngestion.waitForItem(new Predicate<Envelope>() {
            @Override
            public boolean apply(Envelope input) {
                if (!input.getData().getBaseType().equals("RemoteDependencyData")) {
                    return false;
                }
                RemoteDependencyData rdd = (RemoteDependencyData) ((Data) input.getData()).getBaseData();
                // the old agent captured several internal queries, e.g. "SHOW WARNINGS"
                return rdd.getData().equals("select * from abc");
            }
        }, 10, TimeUnit.SECONDS);

        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:mysql://"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/postgresPreparedStatement")
    public void postgresPreparedStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:postgresql://"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/postgresStatement")
    public void postgresStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:postgresql://"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/sqlServerPreparedStatement")
    public void sqlServerPreparedStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:sqlserver://"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/sqlServerStatement")
    public void sqlServerStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:sqlserver://"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Ignore("FIXME: need custom container with oracle db")
    @Test
    @TargetUri("/oraclePreparedStatement")
    public void oraclePreparedStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:oracle:thin:@"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Ignore("FIXME: need custom container with oracle db")
    @Test
    @TargetUri("/oracleStatement")
    public void oracleStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:oracle:thin:@"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    private static void assertSameOperationId(Envelope rdEnvelope, Envelope rddEnvelope) {
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        String operationParentId = rdEnvelope.getTags().get("ai.operation.parentId");

        assertNotNull(operationId);
        assertNotNull(operationParentId);

        assertEquals(operationId, rddEnvelope.getTags().get("ai.operation.id"));
        assertEquals(operationParentId, rddEnvelope.getTags().get("ai.operation.parentId"));
    }
}
