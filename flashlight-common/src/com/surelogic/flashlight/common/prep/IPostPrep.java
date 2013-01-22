package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.flashlight.common.jobs.PrepSLJob;

/**
 * Defines the interface for a database action performed after parsing during
 * raw file preparation.
 * <p>
 * Ensure that any new subclasses are added to the array returned by the
 * {@code getPostPrep} method in {@link PrepSLJob}.
 */
public interface IPostPrep {

    /**
     * Gets a short description of this action to display as it is run.
     * 
     * @return a short description of this action to display as it is run.
     */
    String getDescription();

    /**
     * Runs the database action. This method will only be called after parsing
     * is completed and all thread-local fields and objects have been cleared
     * from the database.
     * 
     * @param c
     *            a connection to the Flashlight database.
     * @param mon
     *            a progress monitor, only to be used to check for cancellation.
     *            Begin and done are invoked around invocation of this method.
     */
    void doPostPrep(final Connection c, final SchemaData schema,
            final SLProgressMonitor mon) throws SQLException;
}
