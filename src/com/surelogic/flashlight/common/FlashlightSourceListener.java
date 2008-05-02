package com.surelogic.flashlight.common;

import com.surelogic.adhoc.SourceListener;
import com.surelogic.adhoc.query.QueryUtil.Cell;
import java.sql.*;
import java.util.Properties;

/**
 *
 * @author Edwin.Chan
 */
public abstract class FlashlightSourceListener extends DataCallable<Void>
implements SourceListener {
    final int run;    
    String pkg;
    String cls;
    int line;
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
        this.columnLabels = labels;
        this.row = r;
        props = new Properties(vars);
        for(int i=0; i<r.length; i++) {
            String key = labels[i].toUpperCase();
            if (!props.containsKey(key)) {
                String value = r[i].label;
                props.put(key, value == null ? "null" : r[i].label);
            }
        }
        if (!props.containsKey("RUN")) {
            props.put("RUN", Integer.toString(run));
        }
        if (listener != null) {
            listener.updateProperties(props);
        }
    }
    
    public <T> void setSource(final String[] columnLabels, final Cell<T>[] row,
                              String pkg, String cls, int line) {
        this.pkg = pkg;
        this.cls = cls;
        this.line = line;
        
        String classId = null;
        for(int i=0; i<columnLabels.length; i++) {
            final String col = columnLabels[i].toLowerCase();
            if ("inclass".equals(col)) {
                classId = row[i].label;    
            }            
            else if (line < 0 && "atline".equals(col)) {
                this.line = Integer.parseInt(row[i].label);
            }
        }
        if (run >= 0 && classId != null) {
            if (query("select PackageName,ClassName from OBJECT"+
                  " where run="+run+" and id="+classId)) {
                setSource(this.pkg, this.cls, this.line);    
            }            
        }                
        setSource(pkg, cls, line);
    }
    
    protected Void handleResultSet(ResultSet rs) throws SQLException {
        pkg = rs.getString(1);
        cls = rs.getString(2);
        return null;
    }
    
    public void setPropertiesListener(IPropertiesListener l) {
        listener = l;
    }
}
