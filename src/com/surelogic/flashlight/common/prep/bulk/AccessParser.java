package com.surelogic.flashlight.common.prep.bulk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.SAXException;

import com.surelogic.common.FileUtility;
import com.surelogic.common.SLUtility;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.prep.ScanRawFileFieldsPreScan;

public class AccessParser extends DefaultResultSet implements
		InvocationHandler, XMLStreamConstants {

	private static final String READ = "field-read";
	private static final String WRITE = "field-write";

	private static final String fileName = "/home/nathan/.flashlight-data/org.gjt.sp.jedit.jEdit-2010.01.11-at-13.51.56.093/org.gjt.sp.jedit.jEdit-2010.01.11-at-13.51.56.093.fl.gz";
	private static final String TIME = "time";

	private final InputStream stream;
	private final XMLStreamReader xmlr;
	private Timestamp wall;
	private long wallNS;
	private final ScanRawFileFieldsPreScan preScan;

	private AccessParser() throws XMLStreamException, IOException,
			ParserConfigurationException, SAXException {
		final File runDocument = new File(fileName);
		stream = new FileInputStream(runDocument);
		final XMLInputFactory xmlif = XMLInputFactory.newInstance();
		if (runDocument.getName().endsWith(FileUtility.GZIP_SUFFIX)) {
			xmlr = xmlif.createXMLStreamReader(new GZIPInputStream(stream));
		} else {
			xmlr = xmlif.createXMLStreamReader(stream);
		}
		preScan = new ScanRawFileFieldsPreScan(new NullSLProgressMonitor(),
				Long.MIN_VALUE, Long.MAX_VALUE);
		final InputStream infoStream = RawFileUtility
				.getInputStreamFor(runDocument);
		try {
			final SAXParser saxParser = RawFileUtility.getParser(runDocument);
			// saxParser.parse(infoStream, preScan);
		} finally {
			infoStream.close();
		}
	}

	private Timestamp ts(final String val) {
		return SLUtility.getWall(wall, wallNS, Long.parseLong(val));
	}

	private static Long lng(final String val) {
		return Long.parseLong(val);
	}

	// ts,inthread,trace,field,rw,receiver,underConstruction
	private Object[] result;
	private boolean wasNull;

	private static Object[] defaultResult() {
		return new Object[] { null, null, null, null, null, null, "N" };
	}

	@Override
	public boolean next() throws SQLException {
		try {
			while (xmlr.hasNext()) {
				if (xmlr.nextTag() == START_ELEMENT) {
					final String name = xmlr.getName().getLocalPart();
					if (TIME.equals(name)) {
						final int count = xmlr.getAttributeCount();
						for (int i = 0; i < count; i++) {
							final String attr = xmlr.getAttributeLocalName(i);
							final String val = xmlr.getAttributeValue(i);
							if ("nano-time".equals(attr)) {
								wallNS = Long.parseLong(val);
							} else if ("wall-clock-time".equals(attr)) {
								final SimpleDateFormat dateFormat = new SimpleDateFormat(
										"yyyy-MM-dd HH:mm:ss.SSS");
								try {
									wall = new Timestamp(dateFormat.parse(val)
											.getTime());
								} catch (final ParseException e) {
									throw new SQLException(e);
								}
							}
						}
					}
					if (READ.equals(name) || WRITE.equals(name)) {
						result = defaultResult();
						result[4] = READ.equals(name) ? "R" : "W";
						final int count = xmlr.getAttributeCount();
						for (int i = 0; i < count; i++) {
							final String attr = xmlr.getAttributeLocalName(i);
							final String val = xmlr.getAttributeValue(i);
							if ("nano-time".equals(attr)) {
								result[0] = ts(val);
							} else if ("thread".equals(attr)) {
								result[1] = lng(val);
							} else if ("trace".equals(attr)) {
								result[2] = lng(val);
							} else if ("field".equals(attr)) {
								result[3] = lng(val);
							} else if ("receiver".equals(attr)) {
								result[5] = lng(val);
							} else if ("under-construction".equals(attr)) {
								result[6] = "yes".equals(val) ? "Y" : "N";
							}
						}
						final long field = (Long) result[3];
						final Long receiver = (Long) result[5];
						return true;
						// if (receiver != null
						// && preScan.isThreadedField(field, receiver)) {
						// return true;
						// }
					}
				}
			}
		} catch (final XMLStreamException e) {
			// FIXME
			// I'm pretty sure there is a better way to do this, but right
			// now this is how we are handling end of document.
		}
		return false;
	}

	@Override
	public long getLong(final int columnIndex) throws SQLException {
		final int i = columnIndex - 1;
		if (i == 5) {
			wasNull = result[5] == null;
			return (Long) (wasNull ? 0L : result[5]);
		}
		return (Long) result[i];
	}

	@Override
	public String getString(final int columnIndex) throws SQLException {
		final int i = columnIndex - 1;
		if (i == 5) {
			wasNull = result[5] == null;
			return (String) (wasNull ? 0L : result[5]);
		}
		return (String) result[i];
	}

	@Override
	public Timestamp getTimestamp(final int columnIndex) throws SQLException {
		final int i = columnIndex - 1;
		if (i == 5) {
			wasNull = result[5] == null;
			return (Timestamp) (wasNull ? 0L : result[5]);
		}
		return (Timestamp) result[i];
	}

	@Override
	public boolean wasNull() throws SQLException {
		return wasNull;
	}

	@Override
	public void close() throws SQLException {
		try {
			xmlr.close();
			stream.close();
		} catch (final XMLStreamException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public Object invoke(final Object proxy, final Method method,
			final Object[] args) throws Throwable {
		final String methodName = method.getName();
		if ("next".equals(methodName)) {
			try {
				while (xmlr.hasNext()) {
					if (xmlr.nextTag() == START_ELEMENT) {
						final String name = xmlr.getName().getLocalPart();
						if (TIME.equals(name)) {
							final int count = xmlr.getAttributeCount();
							for (int i = 0; i < count; i++) {
								final String attr = xmlr
										.getAttributeLocalName(i);
								final String val = xmlr.getAttributeValue(i);
								if ("nano-time".equals(attr)) {
									wallNS = Long.parseLong(val);
								} else if ("wall-clock-time".equals(attr)) {
									final SimpleDateFormat dateFormat = new SimpleDateFormat(
											"yyyy-MM-dd HH:mm:ss.SSS");
									try {
										wall = new Timestamp(dateFormat.parse(
												val).getTime());
									} catch (final ParseException e) {
										throw new SAXException(e);
									}
								}
							}
						}
						if (READ.equals(name) || WRITE.equals(name)) {
							result = defaultResult();
							result[4] = READ.equals(name) ? "R" : "W";
							final int count = xmlr.getAttributeCount();
							for (int i = 0; i < count; i++) {
								final String attr = xmlr
										.getAttributeLocalName(i);
								final String val = xmlr.getAttributeValue(i);
								if ("nano-time".equals(attr)) {
									result[0] = ts(val);
								} else if ("thread".equals(attr)) {
									result[1] = lng(val);
								} else if ("trace".equals(attr)) {
									result[2] = lng(val);
								} else if ("field".equals(attr)) {
									result[3] = lng(val);
								} else if ("receiver".equals(attr)) {
									result[5] = lng(val);
								} else if ("under-construction".equals(attr)) {
									result[6] = "yes".equals(val) ? "Y" : "N";
								}
							}
							return true;
						}
					}
				}
			} catch (final XMLStreamException e) {
				// FIXME
				// I'm pretty sure there is a better way to do this, but right
				// now this is how we are handling end of document.
			}
			return false;
		} else if ("close".equals(methodName)) {
			xmlr.close();
			stream.close();
			return null;
		} else if (methodName.startsWith("get")) {
			final int i = (Integer) args[0] - 1;
			if (i == 5) {
				wasNull = result[5] == null;
				return wasNull ? 0L : result[5];
			}
			return result[i];
		} else if (methodName.startsWith("wasNull")) {
			return wasNull;
		}
		throw new UnsupportedOperationException(method.getName());
	}

	public static ResultSet create() {
		try {
			return new AccessParser();
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
		// try {
		// return (ResultSet) Proxy.newProxyInstance(ResultSet.class
		// .getClassLoader(), new Class[] { ResultSet.class },
		// new AccessParser());
		// } catch (Exception e) {
		// throw new IllegalStateException(e);
		// }
	}

	private static final String TABLE = "CREATE TABLE ACCESS (ID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), TS TIMESTAMP NOT NULL, INTHREAD BIGINT NOT NULL, TRACE BIGINT NOT NULL, FIELD BIGINT NOT NULL, RW CHAR(1) NOT NULL, RECEIVER BIGINT, UNDERCONSTRUCTION CHAR(1) NOT NULL)";
	private static final String FUNC = "CREATE FUNCTION ACCFUNC () RETURNS TABLE (TS TIMESTAMP, INTHREAD BIGINT, TRACE BIGINT, FIELD BIGINT, RW CHAR(1), RECEIVER BIGINT, UNDERCONSTRUCTION CHAR(1)) LANGUAGE JAVA PARAMETER STYLE DERBY_JDBC_RESULT_SET READS SQL DATA EXTERNAL NAME 'com.surelogic.flashlight.common.prep.bulk.AccessParser.create'";
	// We want this: --DERBY-PROPERTIES insertMode=bulkInsert
	private static final String BULK_INSERT = "INSERT INTO ACCESS (TS,INTHREAD,TRACE,FIELD,RW,RECEIVER,UNDERCONSTRUCTION)  SELECT S.* FROM TABLE (ACCFUNC()) S";
	private static final String SINGLE_INSERT = "INSERT INTO ACCESS (TS,INTHREAD,TRACE,FIELD,RW,RECEIVER,UNDERCONSTRUCTION) VALUES (?,?,?,?,?,?,?)";
	private static File BULK_FILE = new File("/home/nathan/tmp/bulk");
	private static File SINGLE_FILE = new File("/home/nathan/tmp/single");
	private static final String BULK_TEST = "jdbc:derby:/home/nathan/tmp/bulk;create=true";
	private static final String BULK_SHUTDOWN = "jdbc:derby:/home/nathan/tmp/bulk;shutdown=true";
	private static final String SINGLE_TEST = "jdbc:derby:/home/nathan/tmp/single;create=true";
	private static final String SINGLE_SHUTDOWN = "jdbc:derby:/home/nathan/tmp/single;shutdown=true";
	private static final String MEMORY_TEST = "jdbc:derby:memory:bulkMem;create=true";
	private static final String MEMORY_SHUTDOWN = "jdbc:derby:memory:bulkMem;shutdown=true";
	private static final String MEMORY_DROP = "DROP TABLE ACCESS";
	private static final String LOCK = "LOCK TABLE ACCESS IN EXCLUSIVE MODE";
	private static final int TESTS = 4;

	public static void main(final String[] args) throws SQLException,
			IOException {
		System.setProperty("derby.system.durability", "test");
		System.setProperty("derby.storage.pageSize", "32768");
		System.setProperty("derby.storage.pageCacheSize", "2000");
		System.out.println("The test begins... now.");
		// Bulk test
		long time = 0;
		for (int i = 0; i < TESTS; i++) {
			time += bulkTest();
		}
		System.out.println("Bulk Average " + time / TESTS);
		// final single test
		time = 0;
		for (int i = 0; i < TESTS; i++) {
			time += prepStTest();
		}
		System.out.println("Prepared statement Average " + time / TESTS);

		time = 0;
		final Connection boot = DriverManager.getConnection(MEMORY_TEST);
		boot.setAutoCommit(false);
		final Statement bootSt = boot.createStatement();
		bootSt.execute(FUNC);
		bootSt.close();
		boot.commit();
		boot.close();
		for (int i = 0; i < TESTS; i++) {
			final Connection c = DriverManager.getConnection(MEMORY_TEST);
			c.setAutoCommit(false);
			Statement st = c.createStatement();
			st.execute(TABLE);
			st.close();
			c.commit();
			final long t = System.currentTimeMillis();
			System.out.println("Table created");
			st = c.createStatement();
			st.execute(BULK_INSERT);
			System.out.println("Executed");
			c.commit();
			System.out.println("Committed");
			final long dur = System.currentTimeMillis() - t;
			System.out.println("Test length: " + dur / 1000 + "." + dur % 1000
					+ "s");
			time += dur;
			st = c.createStatement();
			st.execute(MEMORY_DROP);
			st.close();
			c.commit();
			c.close();
			try {
				DriverManager.getConnection(MEMORY_SHUTDOWN).close();
			} catch (final SQLException e) {
				if (e.getErrorCode() == 45000) {
					SLLogger.getLogger().log(Level.FINE, e.getMessage());
				} else {
					throw new IllegalStateException("The database at "
							+ MEMORY_SHUTDOWN + " did not shut down properly.",
							e);
				}
			}

		}
		System.out.println("In-Memory Average " + time / TESTS);

		time = 0;
		for (int i = 0; i < TESTS; i++) {
			final Connection c = DriverManager.getConnection(MEMORY_TEST);
			c.setAutoCommit(false);
			Statement st = c.createStatement();
			st.execute(TABLE);
			st.close();
			c.commit();
			final long t = System.currentTimeMillis();
			System.out.println("Table created");
			final PreparedStatement p = c.prepareStatement(SINGLE_INSERT);
			final ResultSet set = create();
			int rowCount = 0;
			while (set.next()) {
				rowCount++;
				// ts,inthread,trace,field,rw,receiver,underConstruction
				int idx = 1;
				p.setTimestamp(idx, set.getTimestamp(idx++));
				p.setLong(idx, set.getLong(idx++));
				p.setLong(idx, set.getLong(idx++));
				p.setLong(idx, set.getLong(idx++));
				p.setString(idx, set.getString(idx++));
				p.setLong(idx, set.getLong(idx++));
				p.setString(idx, set.getString(idx++));
				p.addBatch();
			}
			p.executeBatch();
			System.out.println("Executed: " + rowCount + " rows inserted");
			c.commit();
			System.out.println("Committed");
			final long dur = System.currentTimeMillis() - t;
			System.out.println("Test length: " + dur / 1000 + "." + dur % 1000
					+ "s");
			time += dur;
			st = c.createStatement();
			st.execute(MEMORY_DROP);
			st.close();
			c.commit();
			c.close();
			try {
				DriverManager.getConnection(MEMORY_SHUTDOWN).close();
			} catch (final SQLException e) {
				if (e.getErrorCode() == 45000) {
					SLLogger.getLogger().log(Level.FINE, e.getMessage());
				} else {
					throw new IllegalStateException("The database at "
							+ MEMORY_SHUTDOWN + " did not shut down properly.",
							e);
				}
			}

		}
		System.out.println("In-Memory Prepared Statment Average " + time
				/ TESTS);

		// // final ResultSet set = create();
		// // for (int i = 0; i < 3000 && set.next(); i++) {
		// // StringBuilder b = new StringBuilder();
		// // b.append(set.getTimestamp(1));
		// // b.append("\t");
		// // b.append(set.getLong(2));
		// // b.append("\t");
		// // b.append(set.getLong(3));
		// // b.append("\t");
		// // b.append(set.getLong(4));
		// // b.append("\t");
		// // b.append(set.getString(5));
		// // b.append("\t");
		// // long val = set.getLong(6);
		// // b.append(set.wasNull() ? null : val);
		// // b.append("\t");
		// // b.append(set.getString(7));
		// // System.out.println(b.toString());
		// // }
	}

	static long bulkTest() throws SQLException {
		if (BULK_FILE.exists()) {
			FileUtility.recursiveDelete(BULK_FILE);
		}
		final Connection c = DriverManager.getConnection(BULK_TEST);
		c.setAutoCommit(false);
		Statement st = c.createStatement();
		st.execute(FUNC);
		st.execute(TABLE);
		st.close();
		c.commit();
		final long t = System.currentTimeMillis();
		System.out.println("Table created");
		st = c.createStatement();
		st.execute(BULK_INSERT);
		System.out.println("Executed");
		c.commit();
		System.out.println("Committed");
		final long dur = System.currentTimeMillis() - t;
		System.out.println("Test length: " + dur / 1000 + "." + dur % 1000
				+ "s");
		c.close();
		try {
			DriverManager.getConnection(BULK_SHUTDOWN);
		} catch (final SQLException e) {
			if (e.getErrorCode() == 45000) {
				SLLogger.getLogger().log(Level.FINE, e.getMessage());
			} else {
				throw new IllegalStateException("The database at "
						+ BULK_SHUTDOWN + " did not shut down properly.", e);
			}
		}
		return dur;
	}

	static long prepStTest() throws SQLException {
		if (SINGLE_FILE.exists()) {
			FileUtility.recursiveDelete(SINGLE_FILE);
		}
		final Connection c = DriverManager.getConnection(SINGLE_TEST);
		c.setAutoCommit(false);
		final Statement st = c.createStatement();
		st.execute(TABLE);
		st.execute(FUNC);
		st.close();
		c.commit();
		final long t = System.currentTimeMillis();
		System.out.println("Table created");
		final PreparedStatement p = c.prepareStatement(SINGLE_INSERT);
		final ResultSet set = create();
		int rowCount = 0;
		while (set.next()) {
			rowCount++;
			// ts,inthread,trace,field,rw,receiver,underConstruction
			int idx = 1;
			p.setTimestamp(idx, set.getTimestamp(idx++));
			p.setLong(idx, set.getLong(idx++));
			p.setLong(idx, set.getLong(idx++));
			p.setLong(idx, set.getLong(idx++));
			p.setString(idx, set.getString(idx++));
			p.setLong(idx, set.getLong(idx++));
			p.setString(idx, set.getString(idx++));
			p.addBatch();
		}
		p.executeBatch();
		System.out.println("Executed: " + rowCount + " rows inserted");
		c.commit();
		System.out.println("Committed");
		final long dur = System.currentTimeMillis() - t;
		System.out.println("Test length: " + dur / 1000 + "." + dur % 1000
				+ "s");
		c.close();
		try {
			DriverManager.getConnection(SINGLE_SHUTDOWN);
		} catch (final SQLException e) {
			if (e.getErrorCode() == 45000) {
				SLLogger.getLogger().log(Level.FINE, e.getMessage());
			} else {
				throw new IllegalStateException("The database at "
						+ BULK_SHUTDOWN + " did not shut down properly.", e);
			}
		}

		return dur;
	}
}
