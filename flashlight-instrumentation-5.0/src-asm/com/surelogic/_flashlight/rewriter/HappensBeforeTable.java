package com.surelogic._flashlight.rewriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.surelogic._flashlight.common.HappensBeforeConfig;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBefore;
import com.surelogic._flashlight.rewriter.ClassAndFieldModel.ClassNotFoundException;
import com.surelogic._flashlight.rewriter.ClassAndFieldModel.Clazz;

final class HappensBeforeTable {
  private final ClassAndFieldModel classModel;
  
  /* First check the method name.  This is a map from String (method name) to
   * a list of HappensBefore records.  Only contains those records where 
   * the callIn attribute if false.
   */
  private final Map<String, List<Record>> methodMap =
      new HashMap<String, List<Record>>();
  
  /* First check the method name.  This is a map from String (method name) to
   * a list of HappensBefore records.  Only contains those records where 
   * the callIn attribute if true.
   */
  private final Map<String, List<Record>> callInMethodMap =
      new HashMap<String, List<Record>>();
  
  
  
  public HappensBeforeTable(final HappensBeforeConfig config,
      final ClassAndFieldModel m, final RewriteMessenger messenger) {
    classModel = m;
    add(config.getObjects(), messenger);
    add(config.getThreads(), messenger);
    add(config.getCollections(), messenger);
    add(config.getExecutors(), messenger);
  }

  private <T extends HappensBefore> void add(
      final Map<String, List<T>> map, final RewriteMessenger messenger) {
    for (final Map.Entry<String, List<T>> e : map.entrySet()) {
      for (final HappensBefore hb : e.getValue()) {
        final Map<String, List<Record>> mm =
            hb.isCallIn() ? callInMethodMap : methodMap;
        
        final String methodName = hb.getMethod();
        List<Record> records = mm.get(methodName);
        if (records == null) {
          records = new ArrayList<Record>();
          mm.put(methodName, records);
        }
        try {
          records.add(new Record(hb));
        } catch (final ClassNotFoundException ex) {
          /* Couldn't find the class that contains the happens-before method,
           * so we output a warning, and skip it. 
           */
          messenger.warning(
              "Provided classpath is incomplete 6: couldn't find class " +
                  ex.getMissingClass() +
                  " that contains the happens-before method " + hb.getMethod());
        }
      }
    }
  }
  
  
  
  /**
   * Test if the given method call may invoke a happens-before method.
   * 
   * @param internalClassName
   *          The internal class name of the owner of the method call.
   * @param methodName
   *          The name of the method being called.
   * @param methodDesc
   *          The description of the method being called.
   * @return <code>null</code> if the call definitely does not invoke a
   *         happens-before method. Otherwise, an {@link Result} object is
   *         returned. The <code>isExact</code> is <code>true</code> if the
   *         method definitely calls a happens-before method. If
   *         <cod>false</code>, the dynamic type of the receiver needs to be
   *         checked to see if it is assignable to the class type named in the
   *         {@link HappensBefore} object referenced by <code>hb</code>.
   * @throws ClassNotFoundException 
   */
  public Result getHappensBefore(
      final String internalClassName, final String methodName,
      final String methodDesc) throws ClassNotFoundException {
    final List<Record> list = methodMap.get(methodName);
    if (list == null) {
      return null;
    } else {
      for (final Record rec : list) {
        if (methodDesc.startsWith(rec.partialMethodDescriptor)) {
          // match method m(x, y) declared in class Y.
          
          // Is the owner of the called method a class Z such that Z extends Y?
          if (rec.clazz.isAssignableFrom(internalClassName)) {
            // exact match
            return new Result(true, rec.hb);
          }
          
          // Is the owner of the called method a class X such that Y extends X?
          if (classModel.getClass(internalClassName).isAssignableFrom(rec.clazz.getName())) {
            // possible match
            return new Result(false, rec.hb);
          }
        }
      }
      return null;
    }
  }
  
  
  /**
   * Test if the given method description overrides a callIn method.  Meant
   * to be called while a method is being instrumented to see if the 
   * happens-before Store calls should be made.
   * 
   * @param internalClassName
   *          The internal class name of the owner of the method.
   * @param methodName
   *          The name of the method.
   * @param methodDesc
   *          The description of the method.
   * @return <code>null</code> if the call definitely does not invoke a
   *         happens-before method. Returns a happens-before record if the
   *         method definitely overrides a call-in method.
   * @throws ClassNotFoundException 
   */
  public HappensBefore isInsideHappensBefore(
      final String internalClassName, final String methodName,
      final String methodDesc) throws ClassNotFoundException {
    final List<Record> list = callInMethodMap.get(methodName);
    if (list != null) {
      for (final Record rec : list) {
        // match method m(x, y) declared in class Y.
        if (methodDesc.startsWith(rec.partialMethodDescriptor)) {
          /*
           * Is the method being instrumented inside a class that extends (is
           * assignable to) the class Y of the declared happens before method?
           */
          if (rec.clazz.isAssignableFrom(internalClassName)) {
            return rec.hb;
          }
        }
      }
    }
    return null;
  }
  
  
  private final class Record {
    public final Clazz clazz;
    public final String partialMethodDescriptor;
    public final HappensBefore hb;
    
    private Record(final HappensBefore hb) throws ClassNotFoundException {
      this.hb = hb;
      clazz = classModel.getClass(hb.getInternalClass());
      partialMethodDescriptor = hb.getPartialMethodDescriptor();
    }
  }
  
  
  
  public static final class Result {
    public final boolean isExact;
    public final HappensBefore hb;
    
    private Result(final boolean isExact, final HappensBefore hb) {
      this.isExact = isExact;
      this.hb = hb;
    }
  }
}
