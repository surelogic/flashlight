package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.NullResultHandler;
import com.surelogic.common.jdbc.NullRowHandler;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
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
        final Queryable<Void> insert = q
                .prepared("Accesses.prep.lockThreadInfo.insert");
        q.prepared("Accesses.prep.lockThreadInfo.selectForInsert",
                new NullRowHandler() {
                    @Override
                    protected void doHandle(Row r) {
                        insert.call(r.nextLong(), r.nextString(), r.nextLong(),
                                r.nextLong(), r.nextLong());
                    }
                }).call();
        final Queryable<Void> updateJUC = q
                .prepared("Accesses.prep.lockThreadInfo.updateJUC");
        q.prepared("Accesses.prep.lockThreadInfo.selectForUpdateJUC",
                new NullRowHandler() {
                    @Override
                    protected void doHandle(Row r) {
                        updateJUC.call(r.nextLong(), r.nextString(),
                                r.nextLong());
                    }
                }).call();
        final Queryable<Void> updateIntrinsic = q
                .prepared("Accesses.prep.lockThreadInfo.updateIntrinsic");
        q.prepared("Accesses.prep.lockThreadInfo.selectForUpdateIntrinsic",
                new NullRowHandler() {
                    @Override
                    protected void doHandle(Row r) {
                        updateIntrinsic.call(r.nextLong(), r.nextString(),
                                r.nextLong());
                    }
                }).call();
        final Queryable<Void> updateCount = q
                .prepared("Accesses.prep.lockThreadInfo.updateCount");
        q.prepared("Accesses.prep.lockThreadInfo.selectForUpdateCount",
                new NullRowHandler() {

                    @Override
                    protected void doHandle(Row r) {
                        updateCount.call(r.nextLong(), r.nextLong(),
                                r.nextString(), r.nextLong());
                    }
                }).call();
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
        doubleLockedMethods(mon, q);
    }

    private void doubleLockedMethods(SLProgressMonitor mon, final Query q) {
        mon.subTask("Finding double locked methods.");
        final Queryable<?> insert = q
                .prepared("LockInfo.insertDoubleLockedMethod");

        q.prepared("LockInfo.potentialDoubleLockedMethods",
                new NullRowHandler() {
                    long lock;
                    String type;
                    String location;
                    String locationSpec;
                    String locationCode;
                    long inClass;

                    final Queryable<?> test = q.prepared(
                            "LockInfo.testDoubleLockedMethod",
                            new NullResultHandler() {

                                @Override
                                protected void doHandle(Result result) {
                                    for (@SuppressWarnings("unused")
                                    Row r : result) {
                                        insert.call(lock, type, inClass,
                                                location, locationSpec,
                                                locationCode);
                                        return;
                                    }
                                }
                            });

                    @Override
                    protected void doHandle(Row r) {
                        // SELECT L.LOCK, L.TYPE, S.LOCATION, S.LOCATIONSPEC,
                        // S.LOCATIONCODE, S.INCLASS
                        lock = r.nextLong();
                        type = r.nextString();
                        location = r.nextString();
                        locationSpec = r.nextString();
                        locationCode = r.nextString();
                        inClass = r.nextLong();
                        test.call(location, locationSpec, inClass, lock, type,
                                location, locationSpec, inClass, lock, type);

                    }
                }).call();
        mon.subTaskDone();
    }
}
