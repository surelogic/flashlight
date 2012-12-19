package com.surelogic._flashlight.rewriter.config;

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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic._flashlight.rewriter.ByteCodeUtils;
import com.surelogic._flashlight.rewriter.FlashlightNames;
import com.surelogic._flashlight.rewriter.MethodCall;

/**
 * This class parses happens before configuration files for Flashlight.
 * 
 * @author nathan
 * 
 */
public class HappensBeforeConfig {

    private final Map<String, List<HappensBeforeCollection>> collections;
    private final Map<String, List<HappensBeforeObject>> objects;
    private final Map<String, List<HappensBefore>> threads;

    HappensBeforeConfig() {
        collections = new HashMap<String, List<HappensBeforeCollection>>();
        objects = new HashMap<String, List<HappensBeforeObject>>();
        threads = new HashMap<String, List<HappensBefore>>();
    }

    public static HappensBeforeConfig parse(File f) {
        if (f == null || !f.exists() || f.isDirectory()) {
            throw new IllegalArgumentException(f + " is not a valid file name.");
        }
        try {
            SAXParser p = SAXParserFactory.newInstance().newSAXParser();
            HappensBeforeConfigHandler handler = new HappensBeforeConfigHandler();
            p.parse(f, handler);
            return handler.config;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalArgumentException("Could not parse file: " + f, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not parse file: " + f, e);
        }
    }

    public static HappensBeforeConfig parse(InputStream in) {
        try {
            SAXParser p = SAXParserFactory.newInstance().newSAXParser();
            HappensBeforeConfigHandler handler = new HappensBeforeConfigHandler();
            p.parse(in, handler);
            return handler.config;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Map<String, List<HappensBeforeCollection>> getCollections() {
        return collections;
    }

    public Map<String, List<HappensBeforeObject>> getObjects() {
        return objects;
    }

    public Map<String, List<HappensBefore>> getThreads() {
        return threads;
    }

    public List<HappensBeforeCollection> getCollectionHappensBefore(
            String qualifiedClass) {
        return collections.get(qualifiedClass);
    }

    public List<HappensBeforeObject> getObjectHappensBefore(
            String qualifiedClass) {
        return objects.get(qualifiedClass);
    }

    public List<HappensBefore> getThreadHappensBefore(String qualifiedClass) {
        return threads.get(qualifiedClass);
    }

    public static class HappensBeforeObject extends HappensBefore {

        public HappensBeforeObject(String qualifiedClass, String decl,
                HBType type, ReturnCheck returnCheck) {
            super(qualifiedClass, decl, type, returnCheck);
        }

        @Override
        public String toString() {
            return "HappensBeforeObject [getQualifiedClass()="
                    + getQualifiedClass() + ", getSignature()="
                    + getSignature() + ", getType()=" + getType()
                    + ", getReturnCheck()=" + getReturnCheck()
                    + ", getMethod()=" + getMethod() + "]";
        }

        @Override
        public void insertInstrumentation(
            final MethodVisitor mv, final Configuration config,
            final MethodCall mcall, final boolean isExact, final String typeName) {
          // ...
          mcall.pushReceiverForEvent(mv);
          // ..., object
          mcall.pushSiteId(mv);
          // ..., object, callSiteId (long)
          /* Push null if the call is exact, or the qualified type name if
           * the result is not exact.
           */
          if (isExact) {
            mv.visitInsn(Opcodes.ACONST_NULL);
          } else {
            mv.visitLdcInsn(typeName);
          }
          // ..., object, callSideId (long), [type name or null]
          ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.HAPPENS_BEFORE_OBJECT);
          // ...,
        }
    }

    public static class HappensBeforeCollection extends HappensBeforeObject {
        final int objectParam;

        public HappensBeforeCollection(String qualifiedClass, String decl,
                HBType type, ReturnCheck returnCheck, int objectParam) {
            super(qualifiedClass, decl, type, returnCheck);
            this.objectParam = objectParam;
        }

        /**
         * The parameter of this method that corresponds to the object affecting
         * happens-before events, indexed starting at 1. A 0 indicates that it
         * is the return value.
         * 
         * @return
         */
        public int getObjectParam() {
            return objectParam;
        }

        @Override
        public String toString() {
            return "HappensBeforeCollection [objectParam=" + objectParam
                    + ", getQualifiedClass()=" + getQualifiedClass()
                    + ", getSignature()=" + getSignature() + ", getType()="
                    + getType() + ", getReturnCheck()=" + getReturnCheck()
                    + ", getMethod()=" + getMethod() + "]";
        }

        @Override
        public void insertInstrumentation(
            final MethodVisitor mv, final Configuration config,
            final MethodCall mcall, final boolean isExact, final String typeName) {
          /* check if the arg pos is 0, if so, then we use the return value,
           * so we have to push the collection reference down past a copy
           * of the return value.
           */
          if (objectParam == 0) {
            final org.objectweb.asm.Type returnType = mcall.getReturnType();
            if (returnType.equals(org.objectweb.asm.Type.LONG_TYPE)) {
              /* this really shouldn't ever be the case because we expect the
               * return value to be a object reference for our purposes. But
               * let's generate legal JVM code for this case anyhow. 
               */
              // ..., [return value]
              mv.visitInsn(Opcodes.DUP2);
              // ..., [return value], [return value]
              mcall.pushReceiverForEvent(mv);
              // ..., [return value], [return value], collectionRef
              mv.visitInsn(Opcodes.DUP_X2);
              // ..., [return value], collectionRef, [return value], collectionRef
              mv.visitInsn(Opcodes.POP);
              // ..., [return value], collectionRef, [return value]
            } else {
              // ..., [return value]
              mv.visitInsn(Opcodes.DUP);
              // ..., [return value], [return value]
              mcall.pushReceiverForEvent(mv);
              // ..., [return value], [return value], collectionRef
              mv.visitInsn(Opcodes.SWAP);
              // ..., [return value], collectionRef, [return value]
            }
          } else {
            /* Otherwise, push the collection, reference, the site id, and
             * the given actual argument
             */

            // ...
            mcall.pushReceiverForEvent(mv);
            // ..., [return value], collectionRef
            mcall.pushArgumentForEvent(mv, objectParam);
            // ..., [return value], collectionRef, item
          }
          mcall.pushSiteId(mv);
          // ..., [return value], collectionRef, item, callSiteId (long)
          /* Push null if the call is exact, or the qualified type name if
           * the result is not exact.
           */
          if (isExact) {
            mv.visitInsn(Opcodes.ACONST_NULL);
          } else {
            mv.visitLdcInsn(typeName);
          }
          // ..., [return value], collectionRef, item, callSideId (long), [type name or null]
          ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.HAPPENS_BEFORE_COLLECTION);
          // ..., [return value]
        }
    }

    enum HBType {
        SOURCE("source"), TARGET("target"), SOURCEANDTARGET("source-and-target"), FROM(
                "from"), TO("to"), FROMANDTO("from-and-to");

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

    }

    public enum ReturnCheck {
        NONE("none", Opcodes.NOP),
        NOT_NULL("!null", Opcodes.IFNULL),
        NULL("null", Opcodes.IFNONNULL),
        TRUE("true", Opcodes.IFEQ),
        FALSE("false", Opcodes.IFNE);

        private final String name;
        private final int opcode;
        
        private ReturnCheck(final String name, final int opcode) {
            this.name = name;
            this.opcode = opcode;
        }
        
        public int getOpcode() {
          return opcode;
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
        METHOD("method"), THREAD("thread"), OBJECT("object"), COLL("collection");
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
        DECL("decl"), HB("hb"), RESULT_MUST_BE("resultMustBe"), ARG_NUM(
                "argNum"), TYPE("type");
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

    static class HappensBeforeConfigHandler extends DefaultHandler {
        final HappensBeforeConfig config = new HappensBeforeConfig();
        private String curClass;
        private Elem hb;

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            Elem e = Elem.lookup(qName);
            if (e != null) {
                switch (e) {
                case COLL:
                case OBJECT:
                case THREAD:
                    hb = e;
                    curClass = attributes.getValue(Attr.TYPE.name);
                    break;
                case METHOD:
                    String decl = null;
                    HBType type = null;
                    ReturnCheck check = ReturnCheck.NONE;
                    int param = Integer.MIN_VALUE;
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
                            default:
                                throw new IllegalStateException(
                                        "Invalid attribute found.");
                            }
                        }
                    }
                    switch (hb) {
                    case THREAD:
                        add(curClass, new HappensBefore(curClass, decl, type,
                                check), config.threads);
                        break;
                    case COLL:
                        add(curClass, new HappensBeforeCollection(curClass,
                                decl, type, check, param), config.collections);
                        break;
                    case OBJECT:
                        add(curClass, new HappensBeforeObject(curClass, decl,
                                type, check), config.objects);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Parser should never reach here.");
                    }
                    break;
                }
            }

        }

    }

    /**
     * Represents a single method in a class to be instrumented, and also
     * indicates whether or not the return value of the method is to be checked
     * and how the method contributes to a happens-before edge.
     * 
     * @author nathan
     * 
     */
    public static class HappensBefore {
        Pattern DECL_PATTERN = Pattern.compile("(.*)\\((.*)\\)");

        private final String qualifiedClass;
        private final String method;
        private final List<String> signature;
        private final HBType type;
        private final ReturnCheck returnCheck;

        public HappensBefore(String qualifiedClass, String decl, HBType type,
                ReturnCheck returnCheck) {
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
                throw new IllegalArgumentException(decl
                        + " is not a valid declaration.");
            }
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

        @Override
        public String toString() {
            return "HappensBefore [qualifiedClass=" + qualifiedClass
                    + ", method=" + method + ", signature=" + signature
                    + ", type=" + type + ", returnCheck=" + returnCheck + "]";
        }

        public void insertInstrumentation(
            final MethodVisitor mv, final Configuration config,
            final MethodCall mcall, final boolean isExact, final String typeName) {
          // ...
          mcall.pushReceiverForEvent(mv);
          // ..., threadRef
          mcall.pushSiteId(mv);
          // ..., threadRef, callSiteId (long)
          /* Push null if the call is exact, or the qualified type name if
           * the result is not exact.
           */
          if (isExact) {
            mv.visitInsn(Opcodes.ACONST_NULL);
          } else {
            mv.visitLdcInsn(typeName);
          }
          // ..., threadRef, callSideId (long), [type name or null]
          ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.HAPPENS_BEFORE_THREAD);
          // ...,
        }
    }

    @Override
    public String toString() {
        return "HappensBeforeConfig [collections=" + collections + ", objects="
                + objects + ", threads=" + threads + "]";
    }

    private static final Pattern primitive = Pattern
            .compile("(byte|short|int|long|float|double|boolean|char|void)((\\s*\\[\\s*\\]\\s*)*)");
    private static final Pattern array = Pattern.compile("\\s*\\[\\s*\\]\\s*");
    private static final Pattern type = Pattern
            .compile("([^\\[\\s]+)((\\s*\\[\\s*\\]\\s*)*)");
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
        return HappensBeforeConfig
                .parse(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(
                                "com/surelogic/_flashlight/rewriter/config/happens-before-config.xml"));
    }

}