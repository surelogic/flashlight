package com.surelogic.flashlight.common;

import com.surelogic.adhoc.SourceListener;
import com.surelogic.adhoc.query.QueryUtil.Cell;
import com.surelogic.common.logging.SLLogger;
import java.sql.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 *
 * @author Edwin.Chan
 */
public abstract class FlashlightSourceListener 
implements SourceListener, Callable<Void> {
    final int run;    
    String pkg;
    String cls;
    int line;
    String sql;
    
    protected FlashlightSourceListener(int run) {
        this.run = run;
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
    
    private boolean query(String sql) {
        try {
            this.sql = sql;
            Data.getExecutor().submit(this).get();
            return true;
        } catch (InterruptedException ex) {
        // Ignore this
        } catch (ExecutionException ex) {
        // Ignore this
        }
        return false;
    }
    
    public Void call() throws Exception {
        Connection c = Data.getConnection();
        try {
            Statement s = c.createStatement();
            try {
                ResultSet rs = s.executeQuery(sql);
                try {
                    if (rs != null && rs.next()) {
                        pkg = rs.getString(1);
                        cls = rs.getString(2);                        
                    }
                } catch (SQLException e) {
                    SLLogger.log(Level.SEVERE, "Unable to finish: " + sql, e);
                } finally {
                    if (rs != null) rs.close();
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
