package com.surelogic._flashlight.common;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Uses the java.nio channels to implement FileOutputStream
 * 
 * @author edwin
 */
public class FileChannelOutputStream extends OutputStream {
  private final FileChannel channel;
  private final ByteBuffer buffer;

  @SuppressWarnings("resource")
  public FileChannelOutputStream(File file) throws IOException {
    channel = new RandomAccessFile(file, "rw").getChannel();
    // channel = new FileOutputStream(file).getChannel();
    // FIX how does this work?
    // buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 5);
    buffer = ByteBuffer.allocateDirect(32768);
    buffer.clear();
  }

  @Override
  public void write(int b) throws IOException {
    if (buffer.position() == buffer.capacity()) {
      // Need to write out before adding to buffer
      flushBuffer();
    }
    buffer.put((byte) b);
  }

  private void flushBuffer() throws IOException {
    buffer.flip();
    if (channel.isOpen()) {
      channel.write(buffer);
    } else {
      throw new IOException("channel is closed");
    }
    buffer.clear();
  }

  @Override
  public void write(byte b[], int off, int len) throws IOException {
    // Check args
    if (b == null) {
      throw new NullPointerException();
    } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return;
    }

    while (len > 0) {
      final int remaining = buffer.capacity() - buffer.position();
      if (remaining > len) {
        // More than enough space for all of it
        buffer.put(b, off, len);
        break;
      } else {
        // Enough to fill the buffer, so do what we can
        buffer.put(b, off, remaining);
        flushBuffer();
        off += remaining;
      }
      len -= remaining;
    }
  }

  @Override
  public void flush() throws IOException {
    if (channel.isOpen()) {
      flushBuffer();
    }
    // FIX this is very slow
    // channel.force(false);
  }

  @Override
  public void close() throws IOException {
    flush();
    channel.close();
  }

  public static OutputStream create(File file) throws IOException {
    final OutputStream os;
    // if (false) {
    // os = new FileChannelOutputStream(file);
    // return new BufferedOutputStream(os, 32768);
    // } else {
    os = new FileOutputStream(file);
    return new BufferedOutputStream(os, 32768);
    // }
  }

  public static void main(String... args) throws IOException {
    final int size = 1 << 16;
    final int times = 10;
    long buffered = 0, channel = 0;
    for (int i = 0; i < times; i++) {
      channel += test(new BufferedOutputStream(new FileChannelOutputStream(new File("channel.txt")), size));
    }
    for (int i = 0; i < times; i++) {
      buffered += test(new BufferedOutputStream(new FileOutputStream(new File("buffered.txt")), size));
    }
    System.out.println("Time for buffered: " + buffered + " ns");
    System.out.println("Time for channel:  " + channel + " ns");
  }

  static String msg = "Testing ... I want to know if this really worked.  How can I really tell";
  static byte[] buf = (msg + msg).getBytes();

  private static long test(OutputStream s) throws IOException {
    long start = System.nanoTime();
    for (int i = 0; i < 40000; i++) {
      s.write(buf);
    }
    s.close();
    long end = System.nanoTime();
    return end - start;
  }
}
