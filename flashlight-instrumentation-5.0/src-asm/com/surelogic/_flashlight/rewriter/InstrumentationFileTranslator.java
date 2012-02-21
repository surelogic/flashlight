package com.surelogic._flashlight.rewriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class InstrumentationFileTranslator implements Opcodes {

    private static final String[] requiredProperties = new String[] { "FL_RUN",
            "FL_COLLECTION_TYPE", "FL_RUN_FOLDER", "FL_CONSOLE_PORT",
            "FL_OUTPUT_PORT" };

    private InstrumentationFileTranslator() {

    }

    public static void writeProperties(final File propFile, final File classFile)
            throws IOException {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream(propFile);
        try {
            props.load(in);
        } finally {
            in.close();
        }
        writeProperties(props, classFile);
    }

    public static void writeProperties(final Properties props,
            final File classFile) throws IOException {
        List<String> emptyMethods = new ArrayList<String>();
        for (String s : requiredProperties) {
            if (!props.containsKey(s)) {
                emptyMethods.add(s);
            }
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(V1_6, ACC_PUBLIC,
                "com/surelogic/_flashlight/InstrumentationConf", null,
                "java/lang/Object", null);
        defaultConstructor(writer);
        for (String prop : props.stringPropertyNames()) {
            MethodVisitor m = writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "get"
                    + prop, "()Ljava/lang/String;", null, null);
            m.visitCode();
            m.visitLdcInsn(props.getProperty(prop));
            m.visitInsn(ARETURN);
            m.visitMaxs(1, 0);
            m.visitEnd();
        }
        for (String prop : emptyMethods) {
            MethodVisitor m = writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "get"
                    + prop, "()Ljava/lang/String;", null, null);
            m.visitCode();
            m.visitInsn(ACONST_NULL);
            m.visitInsn(ARETURN);
            m.visitMaxs(1, 0);
            m.visitEnd();
        }
        writer.visitEnd();
        FileOutputStream out = new FileOutputStream(classFile);
        try {
            out.write(writer.toByteArray());
        } finally {
            out.close();
        }
    }

    public static void writeSites(final File siteFile, final File classFile)
            throws IOException {
        InputStreamReader in;
        if (siteFile.getName().endsWith("gz")) {
            in = new InputStreamReader(new GZIPInputStream(new FileInputStream(
                    siteFile)));
        } else {
            in = new FileReader(siteFile);
        }
        BufferedReader reader = new BufferedReader(in);
        try {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            writer.visit(V1_6, ACC_PUBLIC,
                    "com/surelogic/_flashlight/SitesConf", null,
                    "java/lang/Object", null);
            defaultConstructor(writer);
            MethodVisitor method = writer.visitMethod(ACC_PUBLIC + ACC_STATIC,
                    "getSiteLines", "()[Ljava/lang/String;", null, null);
            final List<String> lines = new ArrayList<String>();
            for (String line = reader.readLine(); line != null; line = reader
                    .readLine()) {
                lines.add(line);
            }
            method.visitLdcInsn(lines.size());
            method.visitTypeInsn(ANEWARRAY, "java/lang/String");
            int idx = 0;
            for (String line : lines) {
                method.visitInsn(DUP);
                method.visitLdcInsn(idx++);
                method.visitLdcInsn(line);
                method.visitInsn(AASTORE);
            }
            method.visitInsn(ARETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
            writer.visitEnd();
            FileOutputStream out = new FileOutputStream(classFile);
            try {
                out.write(writer.toByteArray());
            } finally {
                out.close();
            }

        } finally {
            reader.close();
        }
    }

    public static void writeFields(final File fieldFile, final File classFile)
            throws IOException {
        InputStreamReader in;
        if (fieldFile.getName().endsWith("gz")) {
            in = new InputStreamReader(new GZIPInputStream(new FileInputStream(
                    fieldFile)));
        } else {
            in = new FileReader(fieldFile);
        }
        BufferedReader reader = new BufferedReader(in);
        try {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            writer.visit(V1_6, ACC_PUBLIC,
                    "com/surelogic/_flashlight/FieldsConf", null,
                    "java/lang/Object", null);
            defaultConstructor(writer);
            MethodVisitor method = writer.visitMethod(ACC_PUBLIC + ACC_STATIC,
                    "getFieldLines", "()[Ljava/lang/String;", null, null);
            final List<String> lines = new ArrayList<String>();
            for (String line = reader.readLine(); line != null; line = reader
                    .readLine()) {
                lines.add(line);
            }
            method.visitLdcInsn(lines.size());
            method.visitTypeInsn(ANEWARRAY, "java/lang/String");
            int idx = 0;
            for (String line : lines) {
                method.visitInsn(DUP);
                method.visitLdcInsn(idx++);
                method.visitLdcInsn(line);
                method.visitInsn(AASTORE);
            }
            method.visitInsn(ARETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
            writer.visitEnd();
            FileOutputStream out = new FileOutputStream(classFile);
            try {
                out.write(writer.toByteArray());
            } finally {
                out.close();
            }

        } finally {
            reader.close();
        }
    }

    public static void defaultConstructor(final ClassWriter writer) {
        MethodVisitor c = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null,
                null);
        c.visitCode();
        c.visitVarInsn(ALOAD, 0);
        c.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        c.visitInsn(RETURN);
        c.visitMaxs(1, 1);
        c.visitEnd();
    }

}
