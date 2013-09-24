package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jobs.SLProgressMonitor;

public class LockPackageCountPostPrep implements IPostPrep {

    @Override
    public String getDescription() {
        return "Getting ackage access counts for locks.";
    }

    @Override
    public void doPostPrep(Connection c, SchemaData schema,
            SLProgressMonitor mon) throws SQLException {
        new ConnectionQuery(c).prepared("Accesses.prep.packageAccessCounts")
                .call();
    }

}
