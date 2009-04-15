package com.surelogic._flashlight.rewriter.xml;

import java.io.File;
import java.io.PrintWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class Test {
  public static void main(final String[] args) {
    try {
      final JAXBContext ctxt = JAXBContext.newInstance(
          Classes.class, ClassRecord.class, MethodRecord.class);
      final Unmarshaller unmarshaller = ctxt.createUnmarshaller();
      
      
      final Classes classes = (Classes) unmarshaller.unmarshal(new File(args[0]));
      
      final PrintWriter pw = new PrintWriter(System.out);
      classes.dump(pw);
      
      
    } catch (final JAXBException e) {
      e.printStackTrace(System.err);
    }
  }
}
