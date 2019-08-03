package com.springbootstartertest.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.*;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/jdbc/hsqldbStatement")
    public void hsqldbStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/jdbc/hsqldbBatchPreparedStatement")
    public void hsqldbBatchPreparedStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("insert into abc (xyz) values (?)", rdd.getData());
        assertEquals(" [Batch of 3]", rdd.getProperties().get("Args"));
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/jdbc/hsqldbBatchStatement")
    public void hsqldbBatchStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertEquals("jdbc:hsqldb:mem:test", rdd.getName());
        assertEquals("insert into abc (xyz) values ('t'); insert into abc (xyz) values ('u');" +
                " insert into abc (xyz) values ('v')", rdd.getData());
        assertEquals(" [Batch]", rdd.getProperties().get("Args"));
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/jdbc/mysqlPreparedStatement")
    public void mysqlPreparedStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        assertThat(rdList, hasSize(1));
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");
        Envelope rddEnvelope = null;
        // the old agent captured several internal queries, e.g. "SHOW WARNINGS"
        for (Envelope loopEnvelope : rddList) {
            RemoteDependencyData loopData = (RemoteDependencyData) ((Data) loopEnvelope.getData()).getBaseData();
            if (loopData.getData().equals("select * from abc where xyz = ?")) {
                rddEnvelope = loopEnvelope;
                break;
            }
        }
        assertNotNull(rddEnvelope);
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:mysql://"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/jdbc/mysqlStatement")
    public void mysqlStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        assertThat(rdList, hasSize(1));
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");
        Envelope rddEnvelope = null;
        // the old agent captured several internal queries, e.g. "SHOW WARNINGS"
        for (Envelope loopEnvelope : rddList) {
            RemoteDependencyData loopData = (RemoteDependencyData) ((Data) loopEnvelope.getData()).getBaseData();
            if (loopData.getData().equals("select * from abc")) {
                rddEnvelope = loopEnvelope;
                break;
            }
        }
        assertNotNull(rddEnvelope);
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:mysql://"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/jdbc/postgresPreparedStatement")
    public void postgresPreparedStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:postgresql://"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/jdbc/postgresStatement")
    public void postgresStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:postgresql://"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/jdbc/sqlServerPreparedStatement")
    public void sqlServerPreparedStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:sqlserver://"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/jdbc/sqlServerStatement")
    public void sqlServerStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:sqlserver://"));
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Ignore("FIXME: need custom container with oracle db")
    @Test
    @TargetUri("/jdbc/oraclePreparedStatement")
    public void oraclePreparedStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("SQL", rdd.getType());
        assertTrue(rdd.getName().startsWith("jdbc:oracle:thin:@"));
        assertEquals("select * from abc where xyz = ?", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Ignore("FIXME: need custom container with oracle db")
    @Test
    @TargetUri("/jdbc/oracleStatement")
    public void oracleStatement() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

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
