package com.android.ide.eclipse.adt.internal.launch;

import java.util.Collection;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ISourceLocator;

import com.android.ddmlib.IDevice;
import com.surelogic.flashlight.client.eclipse.model.RunManager;

/**
 * A version
 *
 * @author nathan
 *
 */
@SuppressWarnings("restriction")
public class InstrumentedAndroidLaunch extends AndroidLaunch {
    String runId;
    private boolean launched;

    public InstrumentedAndroidLaunch(
            ILaunchConfiguration launchConfiguration, String mode,
            ISourceLocator locator) {
        super(launchConfiguration, mode, locator);
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    @Override
    public void stopLaunch() {
        if (runId != null && launched == false) {
            RunManager.getInstance()
            .notifyLaunchCancelledPriorToCollectingData(runId);
        }
        super.stopLaunch();
    }

    public boolean isLaunched() {
        return launched;
    }

    public void setLaunched(boolean launched) {
        this.launched = launched;
    }

    /**
     * stopLaunch is always called, so if the action has been performed then
     * we know that even though stopLaunch is being called we haven't
     * cancelled out.
     *
     * @author nathan
     *
     */
    class InstrumentedLaunchAction implements IAndroidLaunchAction {

        private final IAndroidLaunchAction action;

        InstrumentedLaunchAction(IAndroidLaunchAction action) {
            this.action = action;
        }

        @Override
        public boolean doLaunchAction(DelayedLaunchInfo arg0,
                Collection<IDevice> arg1) {
            try {
                return action.doLaunchAction(arg0, arg1);
            } finally {
                launched = true;
            }
        }

        @Override
        public String getLaunchDescription() {
            return action.getLaunchDescription();
        }

        public boolean isLaunched() {
            return launched;
        }

    }
}