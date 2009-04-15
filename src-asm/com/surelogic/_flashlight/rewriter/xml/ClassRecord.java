package com.surelogic._flashlight.rewriter.xml;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="class")
@XmlAccessorType(XmlAccessType.FIELD)
public final class ClassRecord {
  @XmlAttribute(required=true)
  private String name;
  
  @XmlElement(name="method")
  private List<MethodRecord> methods = new ArrayList<MethodRecord>();

  
  
  public String getName() {
    return name;
  }
  
  public void setName(final String n) {
    name = n;
  }
  
  public List<MethodRecord> getMethods() {
    return methods;
  }
  
  
  public void dump(final PrintWriter pw) {
    pw.println(name);
    for (final MethodRecord mr : methods) {
      mr.dump(pw);
    }
  }
}
