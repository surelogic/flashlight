package com.surelogic._flashlight.rewriter;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DuplicateClasses {
  public static class DuplicateClass {
    public final File classpathEntry;
    public final boolean isInstrumented;
    
    public DuplicateClass(final File where, final boolean isI) {
      classpathEntry = where;
      isInstrumented = isI;
    }
  }
  
  private final Map<String, Set<DuplicateClass>> duplicatedClasses = 
    new HashMap<String, Set<DuplicateClass>>();
  
  
  
  public DuplicateClasses() {
    super();
  }
  
  public boolean hasDuplicatesFor(final String internalClassName) {
    return duplicatedClasses.containsKey(internalClassName);
  }
  
  public void addDuplicate(final String internalClassName,
      final File where, final boolean isInstrumented) {
    Set<DuplicateClass> dups = duplicatedClasses.get(internalClassName);
    if (dups == null) {
      dups = new HashSet<DuplicateClass>();
      duplicatedClasses.put(internalClassName, dups);
    }
    dups.add(new DuplicateClass(where, isInstrumented));
  }
  
  public Map<String, Set<DuplicateClass>> getDuplicates() {
    return Collections.unmodifiableMap(duplicatedClasses);
  }
  
  public Map<String, Set<DuplicateClass>> getInconsistentDuplicates() {
    final Map<String, Set<DuplicateClass>> result = new HashMap<String, Set<DuplicateClass>>();
    for (final Map.Entry<String, Set<DuplicateClass>> entry : duplicatedClasses.entrySet()) {
      if (areDuplicatesInconsistent(entry.getValue())) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }  
  
  public static boolean areDuplicatesInconsistent(final Set<DuplicateClass> dups) {
    boolean disjunction = false;
    boolean conjunction = true;
    for (final DuplicateClass dup : dups) {
      final boolean isInstrumented = dup.isInstrumented;
      disjunction |= isInstrumented;
      conjunction &= isInstrumented;
    }
    return disjunction != conjunction;
  }
}
