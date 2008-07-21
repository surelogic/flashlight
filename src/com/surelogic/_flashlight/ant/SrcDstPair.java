package com.surelogic._flashlight.ant;

public final class SrcDstPair {
  private String srcDir = null;
  private String destDir = null;
  
  
  public SrcDstPair() {
    super();
  }
  
  
  
  public void setSrcdir(final String src) {
    srcDir = src;
  }
  
  public void setDestdir(final String dest) {
    destDir = dest;
  }

  
  
  String getSrcdir() {
    return srcDir;
  }
  
  String getDestdir() {
    return destDir;
  }
}
