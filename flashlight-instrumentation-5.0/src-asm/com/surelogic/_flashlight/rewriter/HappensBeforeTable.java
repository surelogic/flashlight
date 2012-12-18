package com.surelogic._flashlight.rewriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.surelogic._flashlight.rewriter.config.HappensBeforeConfig;
import com.surelogic._flashlight.rewriter.config.HappensBeforeConfig.HappensBefore;

final class HappensBeforeTable {
  /* Most classes aren't going to have methods that are interesting for
   * happens before, so we use a quick hashtable lookup to see if the class has 
   * any interesting methods.  The method lookup can be slower.
   * 
   * internal class name -> list of happens before objects
   */
  private final Map<String, List<? extends HappensBefore>> classMap =
      new HashMap<String, List<? extends HappensBefore>>();
  
  public HappensBeforeTable(final HappensBeforeConfig config) {
    add(config.getObjects());
    add(config.getThreads());
    add(config.getCollections());
  }

  private <T extends HappensBefore> void add(final Map<String, List<T>> map) {
    for (final Map.Entry<String, List<T>> e : map.entrySet()) {
      final String internalClassName = HappensBeforeConfig.getInternalName(e.getKey());
      classMap.put(internalClassName, e.getValue());
    }
  }
  
  public HappensBefore getHappensBefore(
      final String internalClassName, final String methodName,
      final String methodDesc) {
    final List<? extends HappensBefore> records = classMap.get(internalClassName);
    if (records != null) {
      for (final HappensBefore hb : records) {
        if (methodName.equals(hb.getMethod()) &&
            methodDesc.startsWith(hb.getPartialMethodDescriptor())) {
          return hb;
        }
      }
    }
    return null;
  }
}
