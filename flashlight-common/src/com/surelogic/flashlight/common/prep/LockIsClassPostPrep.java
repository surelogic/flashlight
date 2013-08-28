package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jobs.SLProgressMonitor;

public class LockIsClassPostPrep implements IPostPrep {

    @Override
    public String getDescription() {
        return "Adding lock is class information.";
    }

    @Override
    public void doPostPrep(Connection c, SchemaData schema,
            SLProgressMonitor mon) throws SQLException {
        new ConnectionQuery(c).prepared("Flashlight.LockIsClass").call();
    }

}
