package com.microsoft.ajl.simplecalc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

import com.microsoft.ajl.simplecalc.model.BinaryCalculation;
import com.microsoft.ajl.simplecalc.model.BinaryCalculation.TimestampedBinaryCalculation;
import com.microsoft.ajl.simplecalc.model.BinaryOperator;

public class CalculationHistoryService implements ICalculationHistoryService {
	static {
		try {
			Class<?> clazz = Class.forName("com.mysql.jdbc.Driver");
			clazz.newInstance();
		} catch (ClassNotFoundException e) {
			System.err.println("Couldn't find mysql jdbc driver");
			e.printStackTrace();
		} catch (InstantiationException e) {
			System.err.println("Couldn't create instance of mysql jdbc driver");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println("Error loading mysql jdbc driver");
			e.printStackTrace();
		}
	}
	
	private final CalculationDatabaseConfig config;
	private final String table;
	private final String database;

	public CalculationHistoryService() {
		database = "calc_history";
		table = "history";
		String url = String.format(
				"jdbc:mysql://arlittle-test-db.mysql.database.azure.com:3306/%s?verifyServerCertificate=false&useSSL=false&requireSSL=false",
				database);
		String username = "arlittle@arlittle-test-db";
		String password = "Molson1Molson2";
		config = new CalculationDatabaseConfig(url, username, password);
	}

	@Override
	public void addHistoryEntry(BinaryCalculation bc) {
		Connection conn = null;
		try {
			conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement(String.format(
					"INSERT INTO `%s`.`%s` (`timestamp`, `leftOperand`, `rightOperand`, `operator`) VALUES (?, ?, ?, ?)",
					database, table));
			int rowsUpdated = stmt.executeUpdate();
			System.out.printf("Added history entry. Rows updated: %d%n", rowsUpdated);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// don't care
				}
			}
		}
	}
	
	private Connection getConnection() throws SQLException {
		Connection conn = null;
		final int maxTries = 3;
		SQLException lastException = null;
		for (int tries = 0; tries < maxTries; tries++) {
			try {
				conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
				Statement testConn = conn.createStatement();
				testConn.executeQuery(String.format("SELECT 1 FROM `%s`.`%s` WHERE 1=1", database, table));
				return conn;
			} catch (SQLException e) {
				lastException = e;
				System.err.printf("couldn't connect or health check failed: %s%n", e.getLocalizedMessage());
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e1) {
						// don't care
					}
				}
			}
		}
		if (lastException != null) {
			throw lastException;
		}
		throw new IllegalStateException("Could not return connection and had no exception.");
	}

	@Override
	public Collection<TimestampedBinaryCalculation> getHistoryEntries() {
		Connection conn = null;
		try {
			conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT `timestamp`,`leftOperand`,`operator`,`rightOperand` FROM `calc_history`.`history` LIMIT 10");
			ResultSet results = stmt.executeQuery();
			try {
				int resultCount = 0;
				Collection<TimestampedBinaryCalculation> rval = new ArrayList<TimestampedBinaryCalculation>();
				while (results.next()) {
					resultCount++;
					double lo = results.getDouble("leftOperand");
					double ro = results.getDouble("rightOperand");
					String verb = results.getString("operator");
					BinaryOperator op = BinaryOperator.fromVerb(verb);
					if (op == null) {
						System.err.println("found unknown binary operator: " + verb);
						continue;
					}
					BinaryCalculation bc = new BinaryCalculation(lo, ro, op);
					rval.add(new TimestampedBinaryCalculation(bc));
				}
				System.out.printf("database returned %d results. parsed %d results successfully.%n", resultCount, rval.size());
				return rval;
			} finally {
				results.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// don't care
				}
			}
		}
		return null;
	}
}
