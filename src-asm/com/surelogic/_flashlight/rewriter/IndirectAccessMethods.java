package com.surelogic._flashlight.rewriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class IndirectAccessMethods {
  private final Map<String, List<IndirectAccessMethod>> methods;
  
  public IndirectAccessMethods() {
    methods = new HashMap<String, List<IndirectAccessMethod>>();
  }
  
  public void put(final String methodName, final IndirectAccessMethod method) {
    List<IndirectAccessMethod> methodList = methods.get(methodName);
    if (methodList == null) {
      methodList = new ArrayList<IndirectAccessMethod>();
      methods.put(methodName, methodList);
    }
    methodList.add(method);
  }
  
  public IndirectAccessMethod get(
      final String owner, final String name, final String desc) {
    final List<IndirectAccessMethod> methodList = methods.get(name);
    if (methodList == null) {
      return null;
    } else {
      for (final IndirectAccessMethod method : methodList) {
        if (method.matches(owner, name, desc)) {
          return method;
        }
      }
      return null;
    }
  }
}
