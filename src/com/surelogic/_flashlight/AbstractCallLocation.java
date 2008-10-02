package com.surelogic._flashlight;

public abstract class AbstractCallLocation extends ObservationalEvent implements ICallLocation {
    private final ClassPhantomReference f_withinClass;
    private final int f_line;

    protected AbstractCallLocation(final ClassPhantomReference inClass, final int line) {
        f_withinClass = inClass;
        f_line = line;
    }
    
    public final int getLine() {
        return f_line;
    }

    public final long getWithinClassId() {
        return f_withinClass.getId();
    }
    
    String getWithinClassName() {
    	return f_withinClass.getName();
    }
    
    @Override
    public final int hashCode() {
        return (int) (f_withinClass.getId() + f_line);
    }
    
    @Override
    public final boolean equals(Object o) {
        if (o instanceof ICallLocation) {
            ICallLocation bt = (ICallLocation) o;
            return bt.getLine() == f_line &&
                   bt.getWithinClassId() == getWithinClassId();
        }
        return false;
    }
        
    @Override
    public String toString() {
        return "";
    }
    
    protected String superToString() {
    	return super.toString();
    }
    
    @Override
	protected abstract void accept(final EventVisitor v);
}
