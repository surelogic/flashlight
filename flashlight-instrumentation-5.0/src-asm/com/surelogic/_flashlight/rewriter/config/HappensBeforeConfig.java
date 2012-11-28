package com.surelogic._flashlight.rewriter.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class HappensBeforeConfig {

    private final LinkedList<List<HappensBeforeCollection>> collections;
    private final LinkedList<List<HappensBeforeObject>> objects;
    private final LinkedList<List<HappensBefore>> threads;

    HappensBeforeConfig() {
        collections = new LinkedList<List<HappensBeforeCollection>>();
        objects = new LinkedList<List<HappensBeforeObject>>();
        threads = new LinkedList<List<HappensBefore>>();
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

    public List<List<HappensBeforeCollection>> getCollectionHappensBefore() {
        return collections;
    }

    public List<List<HappensBeforeObject>> getObjectHappensBefore() {
        return objects;
    }

    public List<List<HappensBefore>> getThreadHappensBefore() {
        return threads;
    }

    public static class HappensBeforeObject extends HappensBefore {

        public HappensBeforeObject(String qualifiedClass, String method,
                String signature, Type type, ReturnCheck returnCheck) {
            super(qualifiedClass, method, signature, type, returnCheck);
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

        public HappensBeforeCollection(String qualifiedClass, String method,
                String signature, Type type, ReturnCheck returnCheck,
                int objectParam) {
            super(qualifiedClass, method, signature, type, returnCheck);
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
        METHOD("method"), THREAD("happens-before"), OBJECT("happens-before-obj"), COLL(
                "happens-before-coll"), CLASS("class");
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
        NAME("name"), SIG("sig"), TYPE("type"), RETURN("checkReturn"), PARAM(
                "obj");
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

    static class HappensBeforeConfigHandler extends DefaultHandler {
        final HappensBeforeConfig config = new HappensBeforeConfig();
        private final StringBuilder qClass = new StringBuilder();
        private Elem hb;
        private boolean readClass;

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            Elem e = Elem.lookup(qName);
            if (e != null) {
                switch (e) {
                case COLL:
                    List<HappensBeforeCollection> l = new ArrayList<HappensBeforeCollection>();
                    config.collections.add(l);
                    hb = e;
                    qClass.setLength(0);
                    break;
                case OBJECT:
                    List<HappensBeforeObject> lo = new ArrayList<HappensBeforeObject>();
                    config.objects.add(lo);
                    hb = e;
                    qClass.setLength(0);
                    break;
                case THREAD:
                    List<HappensBefore> lt = new ArrayList<HappensBefore>();
                    config.threads.add(lt);
                    hb = e;
                    qClass.setLength(0);
                    break;
                case CLASS:
                    readClass = true;
                    break;
                case METHOD:
                    String name = null;
                    String sig = null;
                    Type type = null;
                    ReturnCheck check = ReturnCheck.NONE;
                    int param = Integer.MIN_VALUE;
                    for (int i = 0; i < attributes.getLength(); i++) {
                        Attr a = Attr.lookup(attributes.getQName(i));
                        String val = attributes.getValue(i);
                        if (a != null) {
                            switch (a) {
                            case NAME:
                                name = val;
                                break;
                            case PARAM:
                                param = Integer.parseInt(val);
                                break;
                            case RETURN:
                                check = ReturnCheck.lookup(val);
                                break;
                            case SIG:
                                sig = val;
                                break;
                            case TYPE:
                                type = Type.lookup(val);
                                break;
                            }
                        }
                    }
                    if (hb == Elem.THREAD) {
                        config.threads.getLast().add(
                                new HappensBefore(qClass.toString(), name, sig,
                                        type, check));
                    } else if (hb == Elem.COLL) {
                        config.collections.getLast().add(
                                new HappensBeforeCollection(qClass.toString(),
                                        name, sig, type, check, param));
                    } else if (hb == Elem.OBJECT) {
                        config.objects.getLast().add(
                                new HappensBeforeObject(qClass.toString(),
                                        name, sig, type, check));
                    }
                    break;
                }
            }

        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            Elem e = Elem.lookup(qName);
            if (e == Elem.CLASS) {
                readClass = false;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (readClass) {
                qClass.append(ch, start, length);
            }
        }

    }

    public static void main(String[] args) {
        HappensBeforeConfig parse = HappensBeforeConfig
                .parse(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(
                                "com/surelogic/_flashlight/rewriter/config/default-flashlight-happensbefore.xml"));
        System.out.println(parse);
    }

    public static class HappensBefore {
        private final String qualifiedClass;
        private final String method;
        private final String signature;
        private final Type type;
        private final ReturnCheck returnCheck;

        public HappensBefore(String qualifiedClass, String method,
                String signature, Type type, ReturnCheck returnCheck) {
            this.qualifiedClass = qualifiedClass;
            this.signature = signature;
            this.type = type;
            this.returnCheck = returnCheck;
            this.method = method;
        }

        public String getQualifiedClass() {
            return qualifiedClass;
        }

        public String getSignature() {
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