package com.surelogic._flashlight.rewriter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DuplicateClasses {
  private final Map<String, Map<String, Boolean>> duplicatedClasses = 
    new HashMap<String, Map<String, Boolean>>();
  
  
  
  public DuplicateClasses() {
    super();
  }
  
  public boolean hasDuplicatesFor(final String internalClassName) {
    return duplicatedClasses.containsKey(internalClassName);
  }
  
  public void addDuplicate(final String internalClassName,
      final File where, final boolean isInstrumented) {
    Map<String, Boolean> dups = duplicatedClasses.get(internalClassName);
    if (dups == null) {
      dups = new HashMap<String, Boolean>();
      duplicatedClasses.put(internalClassName, dups);
    }
    dups.put(where.getAbsolutePath(), isInstrumented);
  }
  
  public Map<String, Map<String, Boolean>> getInconsistentDuplicates() {
    final Map<String, Map<String, Boolean>> result = new HashMap<String, Map<String, Boolean>>();
    for (final Map.Entry<String, Map<String, Boolean>> entry : duplicatedClasses.entrySet()) {
      if (areDuplicatesInconsistent(entry.getValue())) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }  
  
  private static boolean areDuplicatesInconsistent(final Map<String, Boolean> dups) {
    boolean disjunction = false;
    boolean conjunction = true;
    for (final boolean isInstrumented : dups.values()) {
      disjunction |= isInstrumented;
      conjunction &= isInstrumented;
    }
    return disjunction != conjunction;
  }
}
