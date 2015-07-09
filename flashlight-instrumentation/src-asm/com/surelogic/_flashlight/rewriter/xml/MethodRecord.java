package com.surelogic._flashlight.rewriter.xml;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="method")
@XmlAccessorType(XmlAccessType.FIELD)
public final class MethodRecord {
  @XmlAttribute(required=true)
  private String signature;
  
  @XmlAttribute
  private boolean isStatic = false;
  
  @XmlElement(name="arg", required=true)
  private List<Integer> args = new ArrayList<Integer>();

  
  
  public String getSignature() {
    return signature;
  }
  
  public void setSignature(final String s) {
    signature = s;
  }
  
  
  
  public boolean getIsStatic() {
    return isStatic;
  }
  
  public void setIsStatic(final boolean v) {
    isStatic = v;
  }
  
  
  public List<Integer> getArgs() {
    return args;
  }

  public void dump(final PrintWriter pw) {
    if (isStatic) pw.print("static ");
    pw.print(signature);
    pw.print(" [");
    for (final Integer a : args) {
      pw.print(a);
      pw.print(" ");
    }
    pw.println("]");
  }
}
