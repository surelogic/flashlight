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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
                Type type, ReturnCheck returnCheck) {
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

    }

    public static class HappensBeforeCollection extends HappensBeforeObject {
        final int objectParam;

        public HappensBeforeCollection(String qualifiedClass, String decl,
                Type type, ReturnCheck returnCheck, int objectParam) {
            super(qualifiedClass, decl, type, returnCheck);
            this.objectParam = objectParam;
        }

        @Override
        public String toString() {
            return "HappensBeforeCollection [objectParam=" + objectParam
                    + ", getQualifiedClass()=" + getQualifiedClass()
                    + ", getSignature()=" + getSignature() + ", getType()="
                    + getType() + ", getReturnCheck()=" + getReturnCheck()
                    + ", getMethod()=" + getMethod() + "]";
        }

    }

    enum Type {
        SOURCE("source"), TARGET("target"), SOURCEANDTARGET("source-and-target"), FROM(
                "from"), TO("to"), FROMANDTO("from-and-to");

        final String name;

        Type(String name) {
            this.name = name;
        }

        static Type lookup(String key) {
            for (Type e : values()) {
                if (e.name.equals(key)) {
                    return e;
                }
            }
            return null;
        }

    }

    enum ReturnCheck {
        NONE("none"), NOT_NULL("!null"), NULL("null"), TRUE("true"), FALSE(
                "false");

        final String name;

        ReturnCheck(String name) {
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
                    Type type = null;
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
                                type = Type.lookup(val);
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

    public static class HappensBefore {
        Pattern DECL_PATTERN = Pattern.compile("(.*)\\((.*)\\)");

        private final String qualifiedClass;
        private final String method;
        private final List<String> signature;
        private final Type type;
        private final ReturnCheck returnCheck;

        public HappensBefore(String qualifiedClass, String decl, Type type,
                ReturnCheck returnCheck) {
            this.qualifiedClass = qualifiedClass;
            this.type = type;
            this.returnCheck = returnCheck;
            Matcher match = DECL_PATTERN.matcher(decl);
            if (match.matches()) {
                signature = new ArrayList<String>();
                method = match.group(1);
                for (String param : match.group(2).split("[\\s,]+]")) {
                    if (param.length() > 0) {
                        signature.add(param);
                    }
                }
            } else {
                throw new IllegalArgumentException(decl
                        + " is not a valid declaration.");
            }
        }

        public String getQualifiedClass() {
            return qualifiedClass;
        }

        public List<String> getSignature() {
            return signature;
        }

        public Type getType() {
            return type;
        }

        public ReturnCheck getReturnCheck() {
            return returnCheck;
        }

        public String getMethod() {
            return method;
        }

        @Override
        public String toString() {
            return "HappensBefore [qualifiedClass=" + qualifiedClass
                    + ", method=" + method + ", signature=" + signature
                    + ", type=" + type + ", returnCheck=" + returnCheck + "]";
        }

    }

    @Override
    public String toString() {
        return "HappensBeforeConfig [collections=" + collections + ", objects="
                + objects + ", threads=" + threads + "]";
    }

}