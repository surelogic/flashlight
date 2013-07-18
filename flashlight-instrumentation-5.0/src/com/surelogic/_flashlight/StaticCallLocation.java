package com.surelogic._flashlight;

public class StaticCallLocation extends AbstractCallLocation {
    private final long f_withinClassId;
    private final int f_line;
    private final String f_memberName;
    private final int f_memberMod;
    private final String f_fileName;
    private final String f_methodCallName;
    private final String f_methodCallOwner;
    private final String f_methodCallDesc;
    private final int f_methodCallModifier;

    public final long getWithinClassId() {
        return f_withinClassId;
    }

    public final int getLine() {
        return f_line;
    }

    public String getLocationName() {
        return f_memberName;
    }

    public int getLocationMod() {
        return f_memberMod;
    }

    public String getFileName() {
        return f_fileName;
    }

    public String getMethodCallName() {
        return f_methodCallName;
    }

    public String getMethodCallOwner() {
        return f_methodCallOwner;
    }

    public String getMethodCallDesc() {
        return f_methodCallDesc;
    }

    StaticCallLocation(final long siteId, final String memberName,
            final int memberMod, final int line, final String file,
            final long declaringType, final String mcOwner,
            final String mcName, final String mcDesc, int mcModifier) {
        super(siteId);
        f_memberName = memberName;
        f_memberMod = memberMod;
        f_line = line;
        f_withinClassId = declaringType;
        f_fileName = file;
        f_methodCallDesc = mcDesc;
        f_methodCallName = mcName;
        f_methodCallOwner = mcOwner;
        f_methodCallModifier = mcModifier;
    }

    @Override
    protected void accept(final EventVisitor v) {
        v.visit(this);
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("<static-call-location");
        Entities.addAttribute("id", getSiteId(), b);
        Entities.addAttribute("in-class", f_withinClassId, b);
        Entities.addAttribute("line", f_line, b);
        Entities.addAttribute("location", f_memberName, b);
        Entities.addAttribute("location-mod", f_memberMod, b);
        Entities.addAttribute("file", f_fileName, b);
        if (f_methodCallName != null) {
            Entities.addAttribute("method-call-owner", f_methodCallOwner, b);
            Entities.addAttribute("method-call-name", f_methodCallName, b);
            Entities.addAttribute("method-call-desc", f_methodCallDesc, b);
            Entities.addAttribute("method-call-mod", f_methodCallModifier, b);
        }
        b.append("/>");
        return b.toString();
    }
}
