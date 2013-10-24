package com.surelogic.flashlight.common.prep;


public enum LockType {
    INTRINSIC("I"), UTIL("U");

    private final String flag;

    LockType(String flag) {
        this.flag = flag;
    }

    public String getFlag() {
        return flag;
    }

    static LockType fromFlag(String flag) {
        for (LockType t : values()) {
            if (t.flag.equals(flag)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Not a valid flag.");
    }

}
