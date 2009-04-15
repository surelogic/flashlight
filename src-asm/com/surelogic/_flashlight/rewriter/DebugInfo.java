package com.surelogic._flashlight.rewriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Label;

/**
 * Class that holds debug information extracted by a first pass over a class
 * and its methods.  In particular, maintains the local variable information
 * and stack size for each method in the class.
 */
final class DebugInfo {
  public static final class MethodInfo {
    /** The number of local variables */
    private final int numLocals;
    
    /** The stack size */
    private final int stackSize;
    
    /**
     * 2-D array of the {@link #VarInfo} records. The first index is the index
     * of the local variable. The second index is the index of the label being
     * switched to. The record then indicates whether a variable dies at that
     * label, and whether a new variable begins.
     */
    private final Map<Integer, Map<Integer, VarInfo>> varToLabelToInfo;
    
    /**
     * Does the method contain calls to methods that access shared state
     * indirectly?
     */
    private final boolean hasIndirect;
    
    
    
    private MethodInfo(final int num, final int size,
        final Map<Integer, Map<Integer, VarInfo>> records,
        final boolean hasI) {
      numLocals = num;
      stackSize = size;
      varToLabelToInfo = records;
      hasIndirect = hasI;
    }
    
    
    
    public int getNumLocals() {
      return numLocals;
    }
    
    public int getStackSize() {
      return stackSize;
    }
    
    public VarInfo getVariableInfo(final int varIdx, final Integer labelIdx) {
      final Map<Integer, VarInfo> labelToInfo = varToLabelToInfo.get(varIdx);
      if (labelToInfo != null) {
        return labelToInfo.get(labelIdx);
      } else { 
        return null;
      }
    }
    
    public boolean hasIndirectAccess() {
      return hasIndirect;
    }
  }
  
  public static final class VarInfo {
    /**
     * The name of the use that starts at this label, or null if no new
     * use starts.
     */
    private String startsAs = null;

    /**
     * The description of the use that starts at this label, or null if no new
     * use starts.
     */
    private String description = null;
    
    
    
    public String variableStartsAs() {
      return startsAs;
    }
    
    public String variableDescription() {
      return description;
    }
  }
  
  
  
  private final Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();
  private String currentKey;
  
  private final Map<Label, Integer> labels = new HashMap<Label, Integer>();
  private int nextLabel = 0;
  
  private Map<Integer, Map<Integer, VarInfo>> varInfo;
  
  private boolean hasIndirect;
  
  
  /**
   * We are about to receive information for a new method.
   */
  public void newMethod(final String name, final String desc) {
    // Reset the labels
    labels.clear();
    nextLabel = 0;
    varInfo = new HashMap<Integer, Map<Integer, VarInfo>>();
    hasIndirect = false;
    currentKey = name + desc;
  }
  
  /**
   * Visit a new label.
   */
  public void visitLabel(final Label label) {
    // Autobox the index
    labels.put(label, nextLabel++);
  }
  
  /**
   * Visit a local variable clause.
   */
  public void visitLocalVariable(
      final int index, final String name, final String desc,
      final Label start) {
    final Map<Integer, VarInfo> m = getVarInfo(index);
      
    final VarInfo startInfo = getInfo(m, labels.get(start));
    startInfo.startsAs = name;
    startInfo.description = desc;
  }
  
  /**
   * Found an indirect state access.
   */
  public void foundIndirectAccess() {
    hasIndirect = true;
  }
  
  /**
   * Visit the end of the method.
   */
  public void visitSizes(final int numLocals, final int stackSize) {
    final MethodInfo mi = new MethodInfo(
        numLocals, stackSize, varInfo, hasIndirect);
    methods.put(currentKey, mi);
  }
  
  public Map<String, MethodInfo> getMethodList() {
    return Collections.unmodifiableMap(methods);
  }
  
  private Map<Integer, VarInfo> getVarInfo(final Integer varIdx) {
    Map<Integer, VarInfo> infos = varInfo.get(varIdx);
    if (infos == null) {
      infos = new HashMap<Integer, VarInfo>();
      varInfo.put(varIdx, infos);
    }
    return infos;
  }
    
  private static VarInfo getInfo(final Map<Integer, VarInfo> m, final Integer labelIdx) {
    VarInfo vi = m.get(labelIdx);
    if (vi == null) {
      vi = new VarInfo();
      m.put(labelIdx, vi);
    }
    return vi;
  }
}
