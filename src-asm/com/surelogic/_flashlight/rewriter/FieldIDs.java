package com.surelogic._flashlight.rewriter;

import java.util.HashMap;
import java.util.Map;

public final class FieldIDs {
  public static final Integer NOT_FOUND = null;
  
  private int nextID = 0;
  
  private Map<String, Map<String, Integer>> classNameToFieldNameToID =
    new HashMap<String, Map<String, Integer>>();
  
  public FieldIDs() {
    super();
  }
  
  public Integer addField(final String className, final String fieldName) {
    final Integer id = new Integer(nextID++);
    
    Map<String, Integer> fieldName2ID = classNameToFieldNameToID.get(className);
    if (fieldName2ID == null) {
      fieldName2ID = new HashMap<String, Integer>();
      classNameToFieldNameToID.put(className, fieldName2ID);
    }
    fieldName2ID.put(fieldName, id);
    return id;
  }
  
  public Integer getFieldID(final String className, final String fieldName) {
    Map<String, Integer> fieldName2ID = classNameToFieldNameToID.get(className);
    if (fieldName2ID == null) {
      return NOT_FOUND;
    } else {
      final Integer id = fieldName2ID.get(fieldName);
      if (id == null) {
        return NOT_FOUND;
      } else {
        return id;
      }
    }
  }
}
