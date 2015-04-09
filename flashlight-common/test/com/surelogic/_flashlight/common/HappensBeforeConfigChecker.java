package com.surelogic._flashlight.common;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeCollectionRule;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeObjectRule;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeRule;

/**
 * Tool used to try to validate our happens-before-config file. In order to work
 * properly, you need to add a version of android.jar to the run configuration
 * when executing the main method.
 *
 * @author nathan
 *
 */
public class HappensBeforeConfigChecker {

    public static void main(String[] args) {
        HappensBeforeConfig config = HappensBeforeConfig.loadDefault();
        for (Entry<String, List<HappensBeforeObjectRule>> e : config
                .getObjects().entrySet()) {
            check(e);
        }
        for (Entry<String, List<HappensBeforeRule>> e : config.getThreads()
                .entrySet()) {
            check(e);
        }
        for (Entry<String, List<HappensBeforeCollectionRule>> e : config
                .getCollections().entrySet()) {
            check(e);
        }
        System.out.println("Check completed.");
    }

    private static final Pattern primitive = Pattern
            .compile("(byte|short|int|long|float|double|boolean|char|void)((\\s*\\[\\s*\\]\\s*)*)");
    private static final Pattern array = Pattern.compile("\\s*\\[\\s*\\]\\s*");
    private static final Pattern type = Pattern
            .compile("([^\\[\\s]+)((\\s*\\[\\s*\\]\\s*)*)");

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
            m = type.matcher(name);
            m.matches();
            if (m.group(3) == null) {
                return Class.forName(m.group(1));
            } else {
                StringBuilder b = new StringBuilder();
                Matcher arrMatch = array.matcher(m.group(2));
                while (arrMatch.find()) {
                    b.append("[");
                }
                b.append("L");
                b.append(m.group(1));
                b.append(";");
                return Class.forName(b.toString());
            }
        }
    }

    public static <T extends HappensBeforeRule> void checkMethod(
            Entry<String, List<T>> e, Class<?> clazz, T hb) {
        String method = hb.getMethod();
        List<String> signature = hb.getSignature();
        Class<?>[] sigClasses = new Class[signature.size()];
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
            Method m = clazz.getDeclaredMethod(method, sigClasses);
            Class<?> returnType = m.getReturnType();
            switch (hb.getReturnCheck()) {
            case NONE:
                break;
            case FALSE:
            case TRUE:
                if (!(returnType == Boolean.TYPE)
                        || returnType.equals(Boolean.class)) {
                    err(e,
                            String.format(
                                    "Return check wants a boolean but the return type of %s%s in class %s is %s",
                                    method, signature, clazz, returnType));
                }
                break;
            case NOT_NULL:
            case NULL:
                if (returnType.isPrimitive()) {
                    err(e,
                            String.format(
                                    "Return check expects an object for the return type of %s%s in class %s is %s",
                                    method, signature, clazz, returnType));
                }
                break;
            }
            if (hb instanceof HappensBeforeCollectionRule) {
                HappensBeforeCollectionRule hbColl = (HappensBeforeCollectionRule) hb;
                int param = hbColl.getObjectParam();
                boolean valid = false;
                if (param <= sigClasses.length) {
                    Class<?> paramClass = param == -1 ? returnType
                            : sigClasses[param - 1];
                    if (!paramClass.isPrimitive()) {
                        valid = true;
                    }
                }
                if (!valid) {
                    err(e,
                            String.format(
                                    "Object parameter of %s does not correspond to a valid parameter on method %s%s of class %s.",
                                    param, method, signature, clazz));
                }
            }
        } catch (NoSuchMethodException e1) {
            err(e, String.format("%s%s is not a valid method in %s", method,
                    signature, clazz));
        }
    }

    public static <T extends HappensBeforeRule> void check(
            Entry<String, List<T>> e) {
        try {
            Class<?> clazz = declToClass(e.getKey());
            for (T o : e.getValue()) {
                if (o.getId() == null) {
                    err(e, "Missing a valid id.");
                }
                checkMethod(e, clazz, o);
            }
        } catch (ClassNotFoundException e1) {
            err(e, "Class not found");
        } catch (SecurityException e1) {
            throw new IllegalStateException(e1);
        }
    }

    public static <T extends HappensBeforeRule> void err(
            Entry<String, List<T>> e, String message) {
        System.out.printf("Problem with data: %s\n%s\n", message, e.toString());
    }

}
