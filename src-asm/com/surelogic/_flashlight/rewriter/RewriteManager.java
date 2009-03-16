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
  private static enum HowToScan {
    DIR_SCAN_ONLY {
      @Override
      public void scan(final RewriteEngine engine, final File src)
          throws IOException {
        engine.scanDirectory(src, false);
      }
    },
    
    DIR {
      @Override
      public void scan(final RewriteEngine engine, final File src)
          throws IOException {
        engine.scanDirectory(src, true);
      }
    },
    
    JAR_SCAN_ONLY {
      @Override
      public void scan(final RewriteEngine engine, final File src)
          throws IOException {
        engine.scanJar(src, false);
      }
    },
    
    JAR {
      @Override
      public void scan(final RewriteEngine engine, final File src)
          throws IOException {
        engine.scanJar(src, true);
      }
    };    
    
    public abstract void scan(
        RewriteEngine engine, File src) throws IOException;
  }

  private static enum HowToInstrument {
    DIR_TO_DIR(HowToScan.DIR) {
      @Override
      public void instrument(final RewriteEngine engine,
          final File src, final File dest, final String runtime)
          throws IOException {
        engine.rewriteDirectoryToDirectory(src, dest);
      }
    },
    
    DIR_TO_JAR(HowToScan.DIR) {
      @Override
      public void instrument(final RewriteEngine engine,
          final File src, final File dest, final String runtime)
          throws IOException {
        engine.rewriteDirectoryToJar(src, dest, runtime);
      }
    },
    
    JAR_TO_DIR(HowToScan.JAR) {
      @Override
      public void instrument(final RewriteEngine engine,
          final File src, final File dest, final String runtime)
          throws IOException {
        engine.rewriteJarToDirectory(src, dest, runtime);
      }
    },
    
    JAR_TO_JAR(HowToScan.JAR) {
      @Override
      public void instrument(final RewriteEngine engine,
          final File src, final File dest, final String runtime)
          throws IOException {
        engine.rewriteJarToJar(src, dest, runtime);
      }
    };
    
    private HowToInstrument(final HowToScan scan) {
      scanHow = scan;
    }
    
    public final HowToScan scanHow;
    
    public abstract void instrument(
        RewriteEngine engine, File src, File dest, String runtime)
        throws IOException;
  }
  
  
  
  private static class ScannableEntry {
    public final HowToScan scanHow;
    public final File src;
    
    public ScannableEntry(final HowToScan h, final File s) {
      scanHow = h;
      src = s;
    }
    
    public void scan(final RewriteEngine engine) throws IOException {
      scanHow.scan(engine, src);
    }
  }
  
  
  
  private static final class InstrumentableEntry extends ScannableEntry {
    public final HowToInstrument instrumentHow;
    public final File dest;
    public final String runtime;
    
    public InstrumentableEntry(final HowToInstrument instr,
        final File s, final File d, final String r) {
      super(instr.scanHow, s);
      instrumentHow = instr;
      dest = d;
      runtime = r;
    }
    
    public void instrument(final RewriteEngine engine) throws IOException {
      instrumentHow.instrument(engine, src, dest, runtime);
    }
  }
  
  
  
  private final Configuration config;
  private final EngineMessenger messenger;
  private final File fieldsFile;
  private final File sitesFile;
  private final List<ScannableEntry> otherClasspathEntries =
    new LinkedList<ScannableEntry>();
  private final List<InstrumentableEntry> entriesToInstrument =
    new LinkedList<InstrumentableEntry>();
  
  
  
  public RewriteManager(final Configuration c, final EngineMessenger m,
      final File ff, final File sf) {
    config = c;
    messenger = m;
    fieldsFile = ff;
    sitesFile = sf;
  }
  
  
  
  public final void addClasspathDir(final File src) {
    otherClasspathEntries.add(new ScannableEntry(HowToScan.DIR_SCAN_ONLY, src));
  }
  
  public final void addClasspathJar(final File src) {
    otherClasspathEntries.add(new ScannableEntry(HowToScan.JAR_SCAN_ONLY, src));
  }
  
  public final void addDirToDir(final File src, final File dest) {
    entriesToInstrument.add(new InstrumentableEntry(HowToInstrument.DIR_TO_DIR, src, dest, null));
  }
  
  public final void addDirToJar(final File src, final File dest, final String runtime) {
    entriesToInstrument.add(new InstrumentableEntry(HowToInstrument.DIR_TO_JAR, src, dest, runtime));
  }
  
  public final void addJarToJar(final File src, final File dest, final String runtime) {
    entriesToInstrument.add(new InstrumentableEntry(HowToInstrument.JAR_TO_JAR, src, dest, runtime));
  }
  
  public final void addJarToDir(final File src, final File dest, final String runtime) {
    entriesToInstrument.add(new InstrumentableEntry(HowToInstrument.JAR_TO_DIR, src, dest, runtime));
  }
  
  public final void execute() {
    execute(false);
  }
  
  public final void execute(final boolean skipScan) {
    PrintWriter fieldsOut = null;
    PrintWriter sitesOut = null;
    try {
      fieldsOut = new PrintWriter(fieldsFile);
      try {
        sitesOut = new PrintWriter(sitesFile);
        final RewriteEngine engine =
          new RewriteEngine(config, messenger, fieldsOut, sitesOut);
        
        if (!skipScan) {
          for (final ScannableEntry entry : otherClasspathEntries) {
            scan(engine, entry);
          }
          for (final ScannableEntry entry : entriesToInstrument) {
            scan(engine, entry);
          }
        }

        for (final InstrumentableEntry entry : entriesToInstrument) {
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
      } catch (final FileNotFoundException e) {
        exceptionCreatingSitesFile(sitesFile, e);
      } finally {
        if (sitesOut != null) {
          sitesOut.close();
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
  
  private void scan(final RewriteEngine engine, final ScannableEntry entry) {
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
  
  protected abstract void exceptionCreatingSitesFile(File sitesFile, FileNotFoundException e);
}
