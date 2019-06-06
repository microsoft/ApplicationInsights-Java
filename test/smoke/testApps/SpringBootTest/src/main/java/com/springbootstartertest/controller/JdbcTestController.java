package com.springbootstartertest.controller;

import org.hsqldb.jdbc.JDBCDriver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;

@RestController
public class JdbcTestController {

    static {
        try {
            setupHsqldb();
            setupMysql();
            setupPostgres();
            setupSqlServer();
            // setupOracle();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/jdbc/hsqldbPreparedStatement")
    public String hsqldbPreparedStatement() throws Exception {
        Connection connection = getHsqldbConnection();
        executePreparedStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/hsqldbStatement")
    public String hsqldbStatement() throws Exception {
        Connection connection = getHsqldbConnection();
        executeStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/hsqldbBatchPreparedStatement")
    public String hsqldbBatchPreparedStatement() throws Exception {
        Connection connection = getHsqldbConnection();
        executeBatchPreparedStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/hsqldbBatchStatement")
    public String hsqldbBatchStatement() throws Exception {
        Connection connection = getHsqldbConnection();
        executeBatchStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/mysqlPreparedStatement")
    public String mysqlPreparedStatement() throws Exception {
        Connection connection = getMysqlConnection();
        executePreparedStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/mysqlStatement")
    public String mysqlStatement() throws Exception {
        Connection connection = getMysqlConnection();
        executeStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/postgresPreparedStatement")
    public String postgresPreparedStatement() throws Exception {
        Connection connection = getPostgresConnection();
        executePreparedStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/postgresStatement")
    public String postgresStatement() throws Exception {
        Connection connection = getPostgresConnection();
        executeStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/sqlServerPreparedStatement")
    public String sqlServerPreparedStatement() throws Exception {
        Connection connection = getSqlServerConnection();
        executePreparedStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/sqlServerStatement")
    public String sqlServerStatement() throws Exception {
        Connection connection = getSqlServerConnection();
        executeStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/oraclePreparedStatement")
    public String oraclePreparedStatement() throws Exception {
        Connection connection = getOracleConnection();
        executePreparedStatement(connection);
        connection.close();
        return "ok";
    }

    @GetMapping("/jdbc/oracleStatement")
    public String oracleStatement() throws Exception {
        Connection connection = getOracleConnection();
        executeStatement(connection);
        connection.close();
        return "ok";
    }

    private static void executePreparedStatement(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("select * from abc where xyz = ?");
        ps.setString(1, "y");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
        }
        rs.close();
        ps.close();
    }

    private void executeStatement(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("select * from abc");
        while (rs.next()) {
        }
        rs.close();
        statement.close();
    }

    private static void executeBatchPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("insert into abc (xyz) values (?)");
        ps.setString(1, "q");
        ps.addBatch();
        ps.setString(1, "r");
        ps.addBatch();
        ps.setString(1, "s");
        ps.addBatch();
        ps.executeBatch();
        ps.close();
    }

    private void executeBatchStatement(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        statement.addBatch("insert into abc (xyz) values ('t')");
        statement.addBatch("insert into abc (xyz) values ('u')");
        statement.addBatch("insert into abc (xyz) values ('v')");
        statement.executeBatch();
        statement.close();
    }

    private static void setupHsqldb() throws SQLException {
        Connection connection = getHsqldbConnection();
        setup(connection);
        connection.close();
    }

    private static void setupMysql() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection connection = getMysqlConnection();
        setup(connection);
        connection.close();
    }

    private static void setupPostgres() throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection connection = getPostgresConnection();
        setup(connection);
        connection.close();
    }

    private static void setupSqlServer() throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection connection = getSqlServerConnection();
        setup(connection);
        connection.close();
    }

    private static void setupOracle() throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection connection = getOracleConnection();
        setup(connection);
        connection.close();
    }

    private static Connection getHsqldbConnection() throws SQLException {
        return JDBCDriver.getConnection("jdbc:hsqldb:mem:test", null);
    }

    private static Connection getMysqlConnection() throws SQLException {
        String hostname = System.getenv("MYSQL");
        return DriverManager.getConnection("jdbc:mysql://" + hostname + "/mysql", "root", "password");
    }

    private static Connection getPostgresConnection() throws SQLException {
        String hostname = System.getenv("POSTGRES");
        return DriverManager.getConnection("jdbc:postgresql://" + hostname + "/postgres", "postgres", "");
    }

    private static Connection getSqlServerConnection() throws SQLException {
        String hostname = System.getenv("SQLSERVER");
        return DriverManager.getConnection("jdbc:sqlserver://" + hostname, "sa", "Password1");
    }

    private static Connection getOracleConnection() throws SQLException {
        String hostname = System.getenv("ORACLE");
        return DriverManager.getConnection("jdbc:oracle:thin:@" + hostname, "system", "password");
    }

    private static void setup(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            statement.execute("create table abc (xyz varchar(10))");
            statement.execute("insert into abc (xyz) values ('x')");
            statement.execute("insert into abc (xyz) values ('y')");
            statement.execute("insert into abc (xyz) values ('z')");
        } finally {
            statement.close();
        }
    }
}
