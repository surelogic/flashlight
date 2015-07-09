package com.surelogic._flashlight.rewriter.xml;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class Classes {
  @XmlElement(name="class", required=true)
  private List<ClassRecord> classes = new ArrayList<ClassRecord>();
  
  public List<ClassRecord> getClasses() {
    return classes;
  }
  
  
  
  public void dump(final PrintWriter pw) {
    for (final ClassRecord cr : classes) {
      cr.dump(pw);
      pw.println();
    }
    pw.flush();
  }
}
