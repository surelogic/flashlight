package com.surelogic._flashlight.rewriter;

import java.io.PrintWriter;

public class MethodCallIdFactory {

    private final PrintWriter pw;

    private long methodCallSite;
    private long nextId = 0L;

    public MethodCallIdFactory(final PrintWriter pw) {
        this.pw = pw;
    }

    private String className;
    private String methodName;

    public void setSite(final long siteId) {
        methodCallSite = siteId;
    }

    public long getMethodCallId(final String className, final String methodName) {
        this.className = className;
        this.methodName = methodName;
        return nextId++;
    }
}
