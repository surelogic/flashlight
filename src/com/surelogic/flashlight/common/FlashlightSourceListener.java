package com.surelogic.flashlight.common;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import com.surelogic.adhoc.SourceListener;
import com.surelogic.adhoc.query.QueryUtil;
import com.surelogic.adhoc.query.QueryUtil.Cell;

public abstract class FlashlightSourceListener extends DataCallable<Void>
		implements SourceListener {

	static final String PKG_CLS = "pkgClass";
	static final String FIELD_TYPE = "fieldType";

	final int run;
	String pkg;
	String cls;
	int line;
	String fieldType;
	String sql;

	String[] columnLabels;
	@SuppressWarnings("unchecked")
	Cell[] row;
	Properties props;
	private IPropertiesListener listener = null;

	protected FlashlightSourceListener(int run) {
		this.run = run;
	}

	public <T> void setData(String[] labels, Cell<T>[] r, Properties vars) {
		/*
		 * LOG.info("Variables passed in for "+listener); for(Entry e :
		 * vars.entrySet()) { LOG.info("\t"+e.getKey()+" = "+e.getValue()); }
		 */
		this.columnLabels = labels;
		this.row = r;
		props = QueryUtil.copyVariables(vars);
		for (int i = 0; i < r.length; i++) {
			String key = labels[i].toUpperCase();
			if (!props.containsKey(key)) {
				String value = r[i].label;
				props.put(key, value == null ? "null" : r[i].label);
			}
		}
		if (!props.containsKey("RUN")) {
			props.put("RUN", Integer.toString(run));
		}
		/*
		 * LOG.info("Variables sent to "+listener); for(Entry e :
		 * props.entrySet()) { LOG.info("\t"+e.getKey()+" = "+e.getValue()); }
		 */
		if (listener != null) {
			listener.updateProperties(props);
		}
	}

	public <T> void setSource(final String[] columnLabels, final Cell<T>[] row,
			String pkg, String cls, int line) {
		this.pkg = pkg;
		this.cls = cls;
		this.line = line;

		String lockId = null;
		String classId = null;
		String fieldName = null;
		fieldType = null;

		for (int i = 0; i < columnLabels.length; i++) {
			final String col = columnLabels[i].toLowerCase();
			if ("inclass".equals(col)) {
				classId = row[i].label;
			} else if (line < 0 && "atline".equals(col)) {
				this.line = Integer.parseInt(row[i].label);
			}
			/*
			 * else if ("ftype".equals(col) || "declaringtype".equals(col)) {
			 * fieldType = row[i].label; }
			 */
			else if ("field".equals(col) || "fieldname".equals(col)) {
				fieldName = row[i].label;
			} else if ("lock".equals(col)) {
				lockId = row[i].label;
			}
		}
		if (run >= 0) {
			if (classId != null) {
				query(PKG_CLS, "select PackageName,ClassName from OBJECT"
						+ " where run=" + run + " and id=" + classId);
			}
			/*
			 * else if (line < 0 && fieldType != null && fieldType.length() > 0 &&
			 * Character.isDigit(fieldType.charAt(0))) { query(FIELD_TYPE,
			 * "select ClassName from OBJECT"+ " where run="+run+" and
			 * id="+fieldType); if (fieldType != null) { // Check if nested
			 * class int lastSeg = fieldType.indexOf('$'); if (lastSeg >= 0) {
			 * fieldType = fieldType.substring(lastSeg + 1); } } }
			 */
		}

		setSource(this.pkg, this.cls);
		if (/* fieldType != null && */fieldName != null) {
			LOG.info("Trying to find declaration: " + fieldType + " "
					+ fieldName);
			this.line = findSourceLine(fieldType, fieldName);
		}
		setSourceLine(this.line);
	}

	protected abstract int findSourceLine(String fieldType, String fieldName);

	protected Void handleResultSet(ResultSet rs) throws SQLException {
		if (PKG_CLS.equals(getQueryName())) {
			pkg = rs.getString(1);
			cls = rs.getString(2);
		} else if (FIELD_TYPE.equals(getQueryName())) {
			fieldType = rs.getString(1);
		}
		return null;
	}

	public void setPropertiesListener(IPropertiesListener l) {
		listener = l;
	}
}
