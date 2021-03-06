package com.surelogic._flashlight.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class parses happens before configuration files for Flashlight.
 *
 * @author nathan
 *
 */
public final class HappensBeforeConfig {

  final Map<String, List<HappensBeforeCollectionRule>> collections = new HashMap<String, List<HappensBeforeCollectionRule>>();
  final Map<String, List<HappensBeforeObjectRule>> objects = new HashMap<String, List<HappensBeforeObjectRule>>();
  final Map<String, List<HappensBeforeRule>> threads = new HashMap<String, List<HappensBeforeRule>>();
  final Map<String, List<HappensBeforeExecutorRule>> executors = new HashMap<String, List<HappensBeforeExecutorRule>>();

  public HappensBeforeConfig parse(File f) {
    if (!f.exists() || f.isDirectory()) {
      throw new IllegalArgumentException(f + " is not a valid file name.");
    }
    try {
      SAXParser p = SAXParserFactory.newInstance().newSAXParser();
      HappensBeforeConfigHandler handler = new HappensBeforeConfigHandler();
      p.parse(f, handler);
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);
    } catch (SAXException e) {
      throw new IllegalArgumentException("Could not parse file: " + f, e);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse file: " + f, e);
    }
    return this;
  }

  public HappensBeforeConfig parse(InputStream in) {
    try {
      SAXParser p = SAXParserFactory.newInstance().newSAXParser();
      HappensBeforeConfigHandler handler = new HappensBeforeConfigHandler();
      p.parse(in, handler);
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);
    } catch (SAXException e) {
      throw new IllegalArgumentException(e);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    return this;
  }

  public Map<String, List<HappensBeforeCollectionRule>> getCollections() {
    return collections;
  }

  public Map<String, List<HappensBeforeObjectRule>> getObjects() {
    return objects;
  }

  public Map<String, List<HappensBeforeRule>> getThreads() {
    return threads;
  }

  public Map<String, List<HappensBeforeExecutorRule>> getExecutors() {
    return executors;
  }

  public List<HappensBeforeCollectionRule> getCollectionHappensBefore(String qualifiedClass) {
    return collections.get(qualifiedClass);
  }

  public List<HappensBeforeObjectRule> getObjectHappensBefore(String qualifiedClass) {
    return objects.get(qualifiedClass);
  }

  public List<HappensBeforeRule> getThreadHappensBefore(String qualifiedClass) {
    return threads.get(qualifiedClass);
  }

  public static class HappensBeforeObjectRule extends HappensBeforeRule {

    public HappensBeforeObjectRule(String id, String qualifiedClass, String decl, HBType type, ReturnCheck returnCheck,
        boolean callIn) {
      super(id, qualifiedClass, decl, type, returnCheck, callIn);
    }

    @Override
    public void invokeSwitch(final HappensBeforeSwitch s) {
      s.caseHappensBeforeObject(this);
    }

    @Override
    public String toString() {
      return "HappensBeforeObject [getId()=" + getId() + ", getSignature()=" + getSignature() + ", getPartialMethodDescriptor()="
          + getPartialMethodDescriptor() + ", getType()=" + getType() + ", getReturnCheck()=" + getReturnCheck() + ", getMethod()="
          + getMethod() + ", isCallIn()=" + isCallIn() + "]";
    }

  }

  public static class HappensBeforeCollectionRule extends HappensBeforeObjectRule {
    final int objectParam;

    public HappensBeforeCollectionRule(String id, String qualifiedClass, String decl, HBType type, ReturnCheck returnCheck,
        int objectParam, boolean callIn) {
      super(id, qualifiedClass, decl, type, returnCheck, callIn);
      this.objectParam = objectParam;
    }

    /**
     * The parameter of this method that corresponds to the object affecting
     * happens-before events, indexed starting at 1. A -1 indicates that it is
     * the return value. A 0 indicates that is the receiver.
     *
     * @return
     */
    public int getObjectParam() {
      return objectParam;
    }

    public boolean isParamReturnValue() {
      return objectParam == -1;
    }

    public boolean isParamReceiver() {
      return objectParam == 0;
    }

    @Override
    public void invokeSwitch(final HappensBeforeSwitch s) {
      s.caseHappensBeforeCollection(this);
    }

    @Override
    public String toString() {
      return "HappensBeforeCollection [objectParam=" + objectParam + ", getId()=" + getId() + ", getSignature()=" + getSignature()
          + ", getPartialMethodDescriptor()=" + getPartialMethodDescriptor() + ", getType()=" + getType() + ", getReturnCheck()="
          + getReturnCheck() + ", getMethod()=" + getMethod() + ", isCallIn()=" + isCallIn() + "]";
    }

  }

  public static class HappensBeforeExecutorRule extends HappensBeforeCollectionRule {

    public HappensBeforeExecutorRule(String id, String qualifiedClass, String decl, HBType type, ReturnCheck returnCheck,
        int objectParam, boolean callIn) {
      super(id, qualifiedClass, decl, type, returnCheck, objectParam, callIn);
    }

    @Override
    public void invokeSwitch(final HappensBeforeSwitch s) {
      s.caseHappensBeforeExecutor(this);
    }

    @Override
    public String toString() {
      return "HappensBeforeExecutor [getObjectParam()=" + getObjectParam() + ", getId()=" + getId() + ", getSignature()="
          + getSignature() + ", getPartialMethodDescriptor()=" + getPartialMethodDescriptor() + ", getType()=" + getType()
          + ", getReturnCheck()=" + getReturnCheck() + ", getMethod()=" + getMethod() + ", isCallIn()=" + isCallIn() + "]";
    }

  }

  public static enum HBType {
    SOURCE("source"), TARGET("target"), SOURCEANDTARGET("source-and-target"), FROM("from"), TO("to"), FROMANDTO("from-and-to");

    final String name;

    HBType(String name) {
      this.name = name;
    }

    static HBType lookup(String key) {
      for (HBType e : values()) {
        if (e.name.equals(key)) {
          return e;
        }
      }
      return null;
    }

    public boolean isFrom() {
      return this == FROM || this == FROMANDTO;
    }

    public boolean isTo() {
      return this == TO || this == FROMANDTO;
    }

    public boolean isSource() {
      return this == SOURCE || this == SOURCEANDTARGET;
    }

    public boolean isTarget() {
      return this == TARGET || this == SOURCEANDTARGET;
    }

  }

  public enum ReturnCheck {
    NONE("none"), NOT_NULL("!null"), NULL("null"), TRUE("true"), FALSE("false");

    private final String name;

    private ReturnCheck(final String name) {
      this.name = name;
    }

    static ReturnCheck lookup(String key) {
      for (ReturnCheck e : values()) {
        if (e.name.equals(key)) {
          return e;
        }
      }
      return NONE;
    }

  }

  private static enum Elem {
    METHOD("method"), THREAD("thread"), OBJECT("object"), COLL("collection"), EXEC("executor");
    final String name;

    Elem(String name) {
      this.name = name;
    }

    static Elem lookup(String key) {
      for (Elem e : values()) {
        if (e.name.equals(key)) {
          return e;
        }
      }
      return null;
    }
  }

  private static enum Attr {
    DECL("decl"), HB("hb"), RESULT_MUST_BE("resultMustBe"), ARG_NUM("argNum"), TYPE("type"), ID("id"), CALL_IN("callIn");
    final String name;

    Attr(String name) {
      this.name = name;
    }

    static Attr lookup(String key) {
      for (Attr e : values()) {
        if (e.name.equals(key)) {
          return e;
        }
      }
      return null;
    }
  }

  static <T> void add(String key, T elem, Map<String, List<T>> map) {
    List<T> list = map.get(key);
    if (list == null) {
      list = new LinkedList<T>();
      map.put(key, list);
    }
    list.add(elem);
  }

  class HappensBeforeConfigHandler extends DefaultHandler {

    private String curClass;
    private String curId;
    private Elem hb;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      Elem e = Elem.lookup(qName);
      if (e != null) {
        switch (e) {
        case COLL:
        case OBJECT:
        case THREAD:
        case EXEC:
          curClass = attributes.getValue(Attr.TYPE.name);
          hb = e;
          curId = attributes.getValue(Attr.ID.name);
          break;
        case METHOD:
          String decl = null;
          String inClass = curClass;
          HBType type = null;
          ReturnCheck check = ReturnCheck.NONE;
          int param = Integer.MIN_VALUE;
          boolean callIn = false;
          for (int i = 0; i < attributes.getLength(); i++) {
            Attr a = Attr.lookup(attributes.getQName(i));
            String val = attributes.getValue(i);
            if (a != null) {
              switch (a) {
              case DECL:
                decl = val;
                break;
              case ARG_NUM:
                param = Integer.parseInt(val);
                break;
              case RESULT_MUST_BE:
                check = ReturnCheck.lookup(val);
                break;
              case HB:
                type = HBType.lookup(val);
                break;
              case CALL_IN:
                callIn = Boolean.parseBoolean(val);
                break;
              case TYPE:
                inClass = val;
                break;
              default:
                throw new IllegalStateException("Invalid attribute found.");
              }
            }
          }
          switch (hb) {
          case THREAD:
            add(inClass, new HappensBeforeRule(curId, inClass, decl, type, check, callIn), threads);
            break;
          case COLL:
            add(inClass, new HappensBeforeCollectionRule(curId, inClass, decl, type, check, param, callIn), collections);
            break;
          case OBJECT:
            add(inClass, new HappensBeforeObjectRule(curId, inClass, decl, type, check, callIn), objects);
            break;
          case EXEC:
            add(inClass, new HappensBeforeExecutorRule(curId, inClass, decl, type, check, param, callIn), executors);
            break;
          default:
            throw new IllegalStateException("Parser should never reach here.");
          }
          break;
        }
      }

    }
  }

  static final Pattern DECL_PATTERN = Pattern.compile("(.*)\\((.*)\\)");

  /**
   * Represents a single method in a class to be instrumented, and also
   * indicates whether or not the return value of the method is to be checked
   * and how the method contributes to a happens-before edge.
   *
   * @author nathan
   *
   */
  public static class HappensBeforeRule {
    private final String id;
    private final String qualifiedClass;
    private final String method;
    private final List<String> signature;
    private final HBType type;
    private final ReturnCheck returnCheck;
    private final boolean callIn;

    public HappensBeforeRule(String id, String qualifiedClass, String decl, HBType type, ReturnCheck returnCheck, boolean callIn) {
      this.id = id;
      this.qualifiedClass = qualifiedClass;
      this.type = type;
      this.returnCheck = returnCheck;
      Matcher match = DECL_PATTERN.matcher(decl);
      if (match.matches()) {
        signature = new ArrayList<String>();
        method = match.group(1);
        for (String param : match.group(2).split("[,\\s]+")) {
          if (param.length() > 0) {
            signature.add(param);
          }
        }
      } else {
        throw new IllegalArgumentException(decl + " is not a valid declaration.");
      }
      this.callIn = callIn;
    }

    /**
     * Get the JVM class name this method belongs to.
     *
     * @return
     */
    public String getInternalClass() {
      return HappensBeforeConfig.getInternalName(qualifiedClass);
    }

    /**
     * An identifying string for this happens-before rule. Should be passed into
     * the store from the instrumentation.
     *
     * @return
     */
    public String getId() {
      return id;
    }

    /**
     * Get the JLS class name that this method belongs to.
     *
     * @return
     */
    public String getQualifiedClass() {
      return qualifiedClass;
    }

    /**
     * The JLS class names of each parameter in this method.
     *
     * @return
     */
    public List<String> getSignature() {
      return signature;
    }

    /**
     * Returns the method descriptor, minus the type of the return value.
     *
     * @return
     */
    public String getPartialMethodDescriptor() {
      return HappensBeforeConfig.getPartialDescriptor(signature);
    }

    /**
     * The type of happens-before event this corresponds to.
     *
     * @return
     */
    public HBType getType() {
      return type;
    }

    /**
     * Indicates whether or not this return value of this method should be
     * checked, and the appropriate way to check it.
     *
     * @return
     */
    public ReturnCheck getReturnCheck() {
      return returnCheck;
    }

    /**
     * The name of the method
     *
     * @return
     */
    public String getMethod() {
      return method;
    }

    public void invokeSwitch(final HappensBeforeSwitch s) {
      s.caseHappensBefore(this);
    }

    public boolean isCallIn() {
      return callIn;
    }

    @Override
    public String toString() {
      return "HappensBefore [id=" + id + ", qualifiedClass=" + qualifiedClass + ", method=" + method + ", signature=" + signature
          + ", type=" + type + ", returnCheck=" + returnCheck + ", callIn=" + callIn + "]";
    }

  }

  public static interface HappensBeforeSwitch {
    public void caseHappensBefore(HappensBeforeRule hb);

    public void caseHappensBeforeObject(HappensBeforeObjectRule hb);

    public void caseHappensBeforeCollection(HappensBeforeCollectionRule hb);

    public void caseHappensBeforeExecutor(HappensBeforeExecutorRule hb);
  }

  @Override
  public String toString() {
    return "HappensBeforeConfig [collections=" + collections + ", objects=" + objects + ", threads=" + threads + "]";
  }

  private static final Pattern primitive = Pattern
      .compile("(byte|short|int|long|float|double|boolean|char|void)((\\s*\\[\\s*\\]\\s*)*)");
  private static final Pattern array = Pattern.compile("\\s*\\[\\s*\\]\\s*");
  private static final Pattern type = Pattern.compile("([^\\[\\s]+)((\\s*\\[\\s*\\]\\s*)*)");
  private static final Map<String, String> primMap = new HashMap<String, String>();

  static {
    primMap.put("int", "I");
    primMap.put("long", "J");
    primMap.put("double", "D");
    primMap.put("float", "F");
    primMap.put("boolean", "Z");
    primMap.put("char", "C");
    primMap.put("byte", "B");
    primMap.put("short", "S");
    primMap.put("void", "V");
  }

  /**
   * Converts a Java language class name to a Java bytecode class name.
   *
   * @param jlsName
   * @return
   */
  public static String getInternalName(String jlsName) {
    return jlsName.replace('.', '/');
  }

  /**
   * Produces a JVM type descriptor from a JLS type declaration.
   *
   * @param jlsName
   * @return
   */
  public static String getInternalTypeDescriptor(String jlsName) {
    Matcher m = primitive.matcher(jlsName);
    String typeName;
    if (m.matches()) {
      typeName = primMap.get(m.group(1));
    } else {
      m = type.matcher(jlsName);
      m.matches();
      typeName = "L" + getInternalName(m.group(1) + ";");
    }
    if (m.group(3) == null) {
      return typeName;
    } else {
      StringBuilder b = new StringBuilder();
      Matcher arrMatch = array.matcher(m.group(2));
      while (arrMatch.find()) {
        b.append("[");
      }
      b.append(typeName);
      return b.toString();
    }
  }

  /**
   * Converts the list of parameter names in JLS format to a partial bytecode
   * descriptor.
   *
   * @param hb
   * @return
   */
  public static String getPartialDescriptor(List<String> params) {
    StringBuilder b = new StringBuilder();
    b.append("(");
    for (String p : params) {
      b.append(getInternalTypeDescriptor(p));
    }
    b.append(")");
    return b.toString();
  }

  public static HappensBeforeConfig loadDefault() {
    InputStream is = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("com/surelogic/_flashlight/common/happens-before-config.xml");
    // For ant, b/c context class loader is broken by design in ant
    if (is == null) {
      is = HappensBeforeConfig.class.getResourceAsStream("/com/surelogic/_flashlight/common/happens-before-config.xml");
    }

    if (is == null) {
      throw new IllegalStateException("Default happens-before-config.xml could not be found");
    }
    return new HappensBeforeConfig().parse(is);
  }

}