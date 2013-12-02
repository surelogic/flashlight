package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.NullRowHandler;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jobs.SLProgressMonitor;

public class LockInfoPostPrep implements IPostPrep {

    @Override
    public String getDescription() {
        return "Getting package access counts for locks.";
    }

    private void packageAccessCounts(SLProgressMonitor mon, Query q) {
        mon.subTask("Calculating lock package access counts.");
        q.prepared("Accesses.prep.packageAccessCounts").call();
        mon.subTaskDone();
    }

    private void lockThreadInfo(SLProgressMonitor mon, Query q) {
        mon.subTask("Collecting lock thread information.");
        q.prepared("Accesses.prep.lockThreadInfo").call();
        mon.subTaskDone();
    }

    private void updateLockTypeCounts(SLProgressMonitor mon, Query q) {
        mon.subTask("Calculating lock type access counts.");
        final Queryable<?> updateTypes = q
                .prepared("Accesses.prep.updateLockInfoMaxTypes");
        q.prepared("Accesses.prep.selectLockInfoMaxTypes",
                new NullRowHandler() {

                    @Override
                    protected void doHandle(Row r) {
                        updateTypes.call(r.nextLong(), r.nextLong(),
                                r.nextString(), r.nextLong());
                    }
                }).call();
        mon.subTaskDone();
    }

    private void lockPackageAccessCounts(SLProgressMonitor mon, Query q) {
        mon.subTask("Calculating lock package access counts.");
        final Queryable<?> updatePackages = q
                .prepared("Accesses.prep.updateLockInfoMaxPackages");
        q.prepared("Accesses.prep.selectLockInfoMaxPackages",
                new NullRowHandler() {

                    @Override
                    protected void doHandle(Row r) {
                        updatePackages.call(r.nextLong(), r.nextLong(),
                                r.nextString(), r.nextLong());
                    }
                }).call();
        mon.subTaskDone();
    }

    @Override
    public void doPostPrep(Connection c, SchemaData schema,
            SLProgressMonitor mon) throws SQLException {
        Query q = new ConnectionQuery(c);
        packageAccessCounts(mon, q);
        lockThreadInfo(mon, q);
        updateLockTypeCounts(mon, q);
        lockPackageAccessCounts(mon, q);
    }
}
