package com.surelogic._flashlight;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.surelogic._flashlight.common.AttributeType;

public class Environment extends Event {

    @Override
    void accept(EventVisitor v) {
        v.visit(this);
    }

    private static void addProperty(final String key, final AttributeType attr,
            final StringBuilder b) {
        String prop = System.getProperty(key);
        if (prop == null) {
            prop = "UNKNOWN";
        }
        Entities.addAttribute(attr.label(), prop, b);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("  <environment");
        try {
            Entities.addAttribute(AttributeType.HOSTNAME.label(), InetAddress
                    .getLocalHost().getHostName(), b);
        } catch (UnknownHostException e) {
            Entities.addAttribute(AttributeType.HOSTNAME.label(), "unknown", b);
        }
        addProperty("user.name", AttributeType.USER_NAME, b);
        addProperty("java.version", AttributeType.JAVA_VERSION, b);
        addProperty("java.vendor", AttributeType.JAVA_VENDOR, b);
        addProperty("os.name", AttributeType.OS_NAME, b);
        addProperty("os.arch", AttributeType.OS_ARCH, b);
        addProperty("os.version", AttributeType.OS_VERSION, b);
        addProperty("java.library.path", AttributeType.LIBRARY_PATH, b);

        if (StoreConfiguration.isAndroid()) {
            Entities.addAttribute(AttributeType.ANDROID.label(), "true", b);
        }
        Entities.addAttribute(AttributeType.MEMORY_MB.label(), Runtime
                .getRuntime().maxMemory() / (1024L * 1024L), b);
        Entities.addAttribute(AttributeType.CPUS.label(), Runtime.getRuntime()
                .availableProcessors(), b);
        b.append("/>");
        return b.toString();
    }

}
