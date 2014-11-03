package com.android.ide.eclipse.adt.internal.launch;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.surelogic._flashlight.common.InstrumentationConstants;

public class RunId {
    private final String name;
    private final String suffix;

    RunId(String name, Date date) {
        this.name = name;
        suffix = new SimpleDateFormat(InstrumentationConstants.DATE_FORMAT)
        .format(date);
    }

    String getDateSuffix() {
        return suffix;
    }

    String getId() {
        return name + suffix + InstrumentationConstants.ANDROID_LAUNCH_SUFFIX;
    }

    public String getName() {
        return name;
    }
}
