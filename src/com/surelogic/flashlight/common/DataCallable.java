package com.surelogic.flashlight.common;

import com.surelogic.common.logging.SLLogger;
import java.sql.*;
import java.util.concurrent.*;
import java.util.logging.*;

public abstract class DataCallable<T> implements Callable<T> {

	protected static final Logger LOG = SLLogger.getLogger();

	private String name;
	private String sql;

	protected DataCallable() {
		this(null, null);
	}

	protected DataCallable(String name, String sql) {
		this.name = name;
		this.sql = sql;
	}

	protected abstract T handleResultSet(ResultSet rs) throws SQLException;

	public final boolean query(String name, String sql) {
		this.name = name;

		if (sql == null) {
			return false;
		}
		try {
			this.sql = sql;
			Data.getInstance().getExecutor().submit(this).get();
			return true;
		} catch (InterruptedException ex) {
			// Ignore this
		} catch (ExecutionException ex) {
			// Ignore this
		}
		return false;
	}

	public final T query() {
		if (sql == null) {
			return null;
		}
		try {
			return Data.getInstance().getExecutor().submit(this).get();
		} catch (InterruptedException ex) {
			LOG.log(Level.SEVERE, "Interrupted: " + sql, ex);
		} catch (ExecutionException ex) {
			LOG.log(Level.SEVERE, "Problem with " + sql, ex.getCause());
		}
		return null;
	}

	public String getQueryName() {
		return name;
	}

	public final T call() throws Exception {
		Connection c = Data.getInstance().getConnection();
		try {
			Statement s = c.createStatement();
			try {
				ResultSet rs = s.executeQuery(sql);
				try {
					if (rs != null && rs.next()) {
						return handleResultSet(rs);
					}
				} catch (SQLException e) {
					LOG.log(Level.SEVERE, "Unable to finish: " + sql, e);
				} finally {
					if (rs != null)
						rs.close();
				}
			} finally {
				if (s != null) {
					s.close();
				}
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return null;
	}
}
