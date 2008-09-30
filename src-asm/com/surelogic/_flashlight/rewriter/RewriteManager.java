package com.surelogic._flashlight.rewriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * Wrapper around a {@link RewriteEngine} that makes it easier to use for
 * the common cases.
 */
public abstract class RewriteManager {
  private static enum How {
    DIR_TO_DIR {
      @Override
      public void scan(final RewriteEngine engine, final File src)
          throws IOException {
        engine.scanDirectory(src);
      }
      
      @Override
      public void instrument(final RewriteEngine engine,
          final File src, final File dest, final String runtime)
          throws IOException {
        engine.rewriteDirectoryToDirectory(src, dest);
      }
    },
    
    DIR_TO_JAR {
      @Override
      public void scan(final RewriteEngine engine, final File src)
          throws IOException {
        engine.scanDirectory(src);
      }
      
      @Override
      public void instrument(final RewriteEngine engine,
          final File src, final File dest, final String runtime)
          throws IOException {
        engine.rewriteDirectoryToJar(src, dest, runtime);
      }
    },
    
    
    JAR_TO_DIR {
      @Override
      public void scan(final RewriteEngine engine, final File src)
          throws IOException {
        engine.scanJar(src);
      }
      
      @Override
      public void instrument(final RewriteEngine engine,
          final File src, final File dest, final String runtime)
          throws IOException {
        engine.rewriteJarToDirectory(src, dest, runtime);
      }
    },
    
    JAR_TO_JAR {
      @Override
      public void scan(final RewriteEngine engine, final File src)
          throws IOException {
        engine.scanJar(src);
      }
      
      @Override
      public void instrument(final RewriteEngine engine,
          final File src, final File dest, final String runtime)
          throws IOException {
        engine.rewriteJarToJar(src, dest, runtime);
      }
    };
    
    
    public abstract void scan(
        RewriteEngine engine, File src) throws IOException;
    public abstract void instrument(
        RewriteEngine engine, File src, File dest, String runtime)
        throws IOException;
  }
  
  private static final class Entry {
    public final How how;
    public final File src;
    public final File dest;
    public final String runtime;
    
    public Entry(final How h, final File s, final File d, final String r) {
      how = h;
      src = s;
      dest = d;
      runtime = r;
    }
    
    public void scan(final RewriteEngine engine) throws IOException {
      how.scan(engine, src);
    }
    
    public void instrument(final RewriteEngine engine) throws IOException {
      how.instrument(engine, src, dest, runtime);
    }
  }
  
  
  
  private final Configuration config;
  private final EngineMessenger messenger;
  private final File fieldsFile;
  private final List<Entry> entries = new LinkedList<Entry>();
  
  
  
  public RewriteManager(final Configuration c, final EngineMessenger m, final File ff) {
    config = c;
    messenger = m;
    fieldsFile = ff;
  }
  
  
  
  public final void addDirToDir(final File src, final File dest) {
    entries.add(new Entry(How.DIR_TO_DIR, src, dest, null));
  }
  
  public final void addDirToJar(final File src, final File dest, final String runtime) {
    entries.add(new Entry(How.DIR_TO_JAR, src, dest, runtime));
  }
  
  public final void addJarToJar(final File src, final File dest, final String runtime) {
    entries.add(new Entry(How.JAR_TO_JAR, src, dest, runtime));
  }
  
  public final void addJarToDir(final File src, final File dest, final String runtime) {
    entries.add(new Entry(How.JAR_TO_DIR, src, dest, runtime));
  }
  
  public final void execute() {
    execute(false);
  }
  
  public final void execute(final boolean skipScan) {
    PrintWriter fieldsOut = null;
    try {
      fieldsOut = new PrintWriter(fieldsFile);
      final RewriteEngine engine = new RewriteEngine(config, messenger, fieldsOut);
      
      if (!skipScan) {
        for (final Entry entry : entries) {
          final String srcPath = entry.src.getAbsolutePath();
          messenger.info("Scanning classfiles in " + srcPath);
          messenger.increaseNesting();
          preScan(srcPath);
          try {
            entry.scan(engine);
          } catch (final IOException e) {
            exceptionScan(srcPath, e);
          } finally {
            messenger.decreaseNesting();
            postScan(srcPath);
          }
        }
      }

      for (final Entry entry : entries) {
        final String srcPath = entry.src.getAbsolutePath();
        final String destPath = entry.dest.getAbsolutePath();
        messenger.info("Instrumenting classfiles in " + srcPath + " to " + destPath);
        messenger.increaseNesting();
        preInstrument(srcPath, destPath);
        try {
          entry.instrument(engine);
        } catch (final IOException e) {
          exceptionInstrument(srcPath, destPath, e);
        } finally {
          messenger.decreaseNesting();
          postInstrument(srcPath, destPath);
        }
      }
    } catch(final FileNotFoundException e) {
      exceptionCreatingFieldsFile(fieldsFile, e);
    } finally {
      if (fieldsOut != null) {
        fieldsOut.close();
      }
    }
  }
  
  protected void preScan(final String srcPath) {
    // do nothing
  }
  
  protected void postScan(final String srcPath) {
    // do nothing
  }
  
  protected abstract void exceptionScan(String srcPath, IOException e);
  
  protected void preInstrument(final String srcPath, final String destPath) {
    // do nothing
  }
  
  protected void postInstrument(final String srcPath, final String destPath) {
    // do nothing
  }
  
  protected abstract void exceptionInstrument(String srcPath, String destPath, IOException e);
  
  protected abstract void exceptionCreatingFieldsFile(File fieldsFile, FileNotFoundException e);
}
