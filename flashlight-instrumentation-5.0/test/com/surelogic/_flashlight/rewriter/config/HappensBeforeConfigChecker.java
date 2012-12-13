package com.surelogic._flashlight.rewriter.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.surelogic._flashlight.rewriter.config.HappensBeforeConfig.HappensBefore;
import com.surelogic._flashlight.rewriter.config.HappensBeforeConfig.HappensBeforeCollection;
import com.surelogic._flashlight.rewriter.config.HappensBeforeConfig.HappensBeforeObject;

public class HappensBeforeConfigChecker {

    public static void main(String[] args) {
        HappensBeforeConfig config = HappensBeforeConfig
                .parse(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(
                                "com/surelogic/_flashlight/rewriter/config/happens-before-config.xml"));
        for (Entry<String, List<HappensBeforeObject>> e : config.getObjects()
                .entrySet()) {
            check(e);
        }
        for (Entry<String, List<HappensBefore>> e : config.getThreads()
                .entrySet()) {
            check(e);
        }
        for (Entry<String, List<HappensBeforeCollection>> e : config
                .getCollections().entrySet()) {
            check(e);
        }
        System.out.println("Check completed.");
    }

    private static final Pattern primitive = Pattern
            .compile("(byte|short|int|long|float|double|boolean|char)((\\s*\\[\\s*\\]\\s*)*)");
    private static final Pattern array = Pattern.compile("\\s*\\[\\s*\\]\\s*");

    private static final Map<String, String> primMap = new HashMap<String, String>();
    private static final Map<String, Class<?>> primClassMap = new HashMap<String, Class<?>>();
    static {
        primMap.put("int", "I");
        primMap.put("long", "J");
        primMap.put("double", "D");
        primMap.put("float", "F");
        primMap.put("boolean", "Z");
        primMap.put("char", "C");
        primMap.put("byte", "B");
        primMap.put("short", "S");
        primClassMap.put("int", Integer.TYPE);
        primClassMap.put("long", Long.TYPE);
        primClassMap.put("double", Double.TYPE);
        primClassMap.put("float", Float.TYPE);
        primClassMap.put("boolean", Boolean.TYPE);
        primClassMap.put("char", Character.TYPE);
        primClassMap.put("byte", Byte.TYPE);
        primClassMap.put("short", Short.TYPE);
        primClassMap.put("void", Void.TYPE);
    }

    static Class<?> declToClass(String name) throws ClassNotFoundException {
        Matcher m = primitive.matcher(name);
        if (m.matches()) {
            if (m.group(3) == null) {
                // Not an array
                return primClassMap.get(m.group(1));
            } else {
                StringBuilder b = new StringBuilder();
                Matcher arrMatch = array.matcher(m.group(2));
                while (arrMatch.find()) {
                    b.append("[");
                }
                b.append(primMap.get(m.group(1)));
                return Class.forName(b.toString());
            }
        } else {
            return Class.forName(name);
        }
    }

    public static <T extends HappensBefore> void checkMethod(
            Entry<String, List<T>> e, Class clazz, T hb) {
        String method = hb.getMethod();
        List<String> signature = hb.getSignature();
        Class[] sigClasses = new Class[signature.size()];
        try {
            for (int i = 0; i < sigClasses.length; i++) {
                try {
                    sigClasses[i] = declToClass(signature.get(i));
                } catch (ClassNotFoundException e1) {
                    err(e,
                            String.format(
                                    "The parameters of method %s in %s are not valid: %s is not a valid class",
                                    method, clazz, signature.get(i)));
                    return;
                }
            }
            clazz.getMethod(method, sigClasses);
        } catch (NoSuchMethodException e1) {
            err(e, String.format("%s%s is not a valid method in %s", method,
                    signature, clazz));
        }
    }

    public static <T extends HappensBefore> void check(Entry<String, List<T>> e) {
        try {
            Class clazz = declToClass(e.getKey());
            for (T o : e.getValue()) {
                checkMethod(e, clazz, o);
            }
        } catch (ClassNotFoundException e1) {
            err(e, "Class not found");
        } catch (SecurityException e1) {
            throw new IllegalStateException(e1);
        }

    }

    public static <T extends HappensBefore> void err(Entry<String, List<T>> e,
            String message) {
        System.out.printf("Problem with data: %s\n%s\n", message, e.toString());
    }

}