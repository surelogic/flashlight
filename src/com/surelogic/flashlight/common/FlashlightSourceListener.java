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
    Cell[] row;
    Properties props;
            
    protected FlashlightSourceListener(int run) {
        this.run = run;
    }
    
    public <T> void setData(final String[] labels, final Cell<T>[] r) {
        this.columnLabels = labels;
        this.row = r;
        props = new Properties();
        for(int i=0; i<r.length; i++) {
            props.put(labels[i].toUpperCase(), r[i].label);
        }
        if (!props.containsKey("RUN")) {
            props.put("RUN", Integer.toString(run));
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
}
