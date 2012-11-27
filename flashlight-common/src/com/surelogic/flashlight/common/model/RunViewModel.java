package com.surelogic.flashlight.common.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.surelogic.common.CommonImages;
import com.surelogic.common.FileUtility;
import com.surelogic.common.Justification;
import com.surelogic.common.SLUtility;
import com.surelogic.flashlight.common.files.RawFileHandles;

/**
 * IDE independent data model for the table of Flashlight runs displayed in the
 * run view. The columns of this table are shown in the list below:
 * 
 * <ul>
 * <li>Size</li>
 * <li>Run</li>
 * <li>Time</li>
 * <li>By</li>
 * <li>Java</li>
 * <li>Vendor</li>
 * <li>OS</li>
 * <li>Max Memory (MB)</li>
 * <li>Processors</li>
 * </ul>
 */
public final class RunViewModel {

  /**
   * Gets the title for the passed column.
   * 
   * @param column
   *          the column index.
   * @return the non-null title for the passed column.
   */
  public String getColumnTitle(final int column) {
    return f_columnData.get(column).getColumnTitle();
  }

  /**
   * Gets the column justification for the passed column.
   * 
   * @param column
   *          the column index.
   * @return the column justification for the passed column.
   */
  public Justification getColumnJustification(final int column) {
    return f_columnData.get(column).getColumnJustification();
  }

  /**
   * Gets the text for the passed row and column.
   * 
   * @param rowData
   *          the data for the row.
   * @param column
   *          the column index.
   * @return the non-null text for the passed row and column.
   */
  public String getText(final RunDescription rowData, final int column) {
    return f_columnData.get(column).getText(rowData);
  }

  /**
   * Gets the symbolic name from {@link CommonImages} or {@code null} if no
   * image should be displayed for the passed row data and column.
   * 
   * @param rowData
   *          the data for the row.
   * @param column
   *          the column index.
   * @return a symbolic name from {@link CommonImages} or {@code null} if no
   *         image should be displayed.
   */
  public String getImageSymbolicName(final RunDescription rowData, final int column) {
    return f_columnData.get(column).getImageSymbolicName(rowData);
  }

  /**
   * Gets the {@link Comparator} used for sorting the passed column.
   * 
   * @param column
   *          the column index.
   * @return the non-null {@link Comparator} used for sorting the passed column.
   */
  public Comparator<RunDescription> getColumnComparator(final int column) {
    return f_columnData.get(column).getColumnComparator();
  }

  /**
   * Returns the number of columns defined by this model.
   * 
   * @return the number of columns defined by this model.
   */
  public int getColumnCount() {
    return f_columnData.size();
  }

  private static abstract class ColumnDataAdaptor {

    abstract String getColumnTitle();

    Justification getColumnJustification() {
      return Justification.LEFT;
    }

    String getText(final RunDescription rowData) {
      return "";
    }

    String getImageSymbolicName(final RunDescription rowData) {
      return null;
    }

    private final Comparator<RunDescription> f_defaultComparator = new Comparator<RunDescription>() {
      @Override
      public int compare(final RunDescription o1, final RunDescription o2) {
        return getText(o1).compareToIgnoreCase(getText(o2));
      }
    };

    Comparator<RunDescription> getColumnComparator() {
      return f_defaultComparator;
    }
  }

  private final List<ColumnDataAdaptor> f_columnData = new ArrayList<ColumnDataAdaptor>();

  public RunViewModel() {
    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Size";
      }

      @Override
      String getText(final RunDescription rowData) {
        if (rowData == null || rowData.getRunDirectory() == null) {
          return "Unknown";
        }
        return rowData.getRunDirectory().getHumanReadableSize();
      }

      @Override
      String getImageSymbolicName(final RunDescription rowData) {
        if (rowData.isPrepared()) {
          return CommonImages.IMG_DRUM;
        }
        return CommonImages.IMG_FILE;
      }

      private final Comparator<RunDescription> f_defaultComparator = new Comparator<RunDescription>() {
        @Override
        public int compare(final RunDescription o1, final RunDescription o2) {
          long size1 = FileUtility.recursiveSizeInBytes(o1.getRunDirectory().getRunDirectory());
          long size2 = FileUtility.recursiveSizeInBytes(o2.getRunDirectory().getRunDirectory());
          if (size1 < size2) {
            return -1;
          } else if (size1 == size2) {
            return 0;
          } else {
            return 1;
          }
        }
      };

      @Override
      Comparator<RunDescription> getColumnComparator() {
        return f_defaultComparator;
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Run";
      }

      @Override
      String getText(final RunDescription rowData) {
        return rowData.getName();
      }

      @Override
      String getImageSymbolicName(final RunDescription rowData) {
        RawFileHandles handles = rowData.getRawFileHandles();
        if (handles != null) {
          if (!handles.isLogClean()) {
            return CommonImages.IMG_WARNING;
          }
        }
        return null; // a hack in the viewer handles this
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Time";
      }

      @Override
      String getText(final RunDescription rowData) {
        return SLUtility.toStringHMS(rowData.getStartTimeOfRun());
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Duration";
      }

      @Override
      String getText(final RunDescription rowData) {
        long duration = rowData.getDuration();
        String text;
        if (duration == 0) {
          text = "-";
        } else if (duration < 1e6) {
          text = String.format("%dns", duration);
        } else if (duration < 1e9) {
          text = String.format("%4.3fs", duration / 1e9);
        } else if (duration < 60 * 1e9) {
          text = String.format("%.3fs", duration / 1e9);
        } else if (duration < 60 * 60 * 1e9) {
          int ms = (int) (duration / 1e9 / 60);
          int s = (int) (duration / 1e9) - 60 * ms;
          text = String.format("%dm %ds", ms, s);
        } else {
          int hs = (int) (duration / 1e9 / 60 / 60);
          int ms = (int) (duration / 1e9 / 60) - 60 * hs;
          int s = (int) (duration / 1e9) - 60 * ms - 60 * 60 * hs;
          text = String.format("%dh %dm %ds", hs, ms, s);
        }
        return text;
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "By";
      }

      @Override
      String getText(final RunDescription rowData) {
        return rowData.getUserName();
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Host";
      }

      @Override
      String getText(final RunDescription rowData) {
        return rowData.getHostname();
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Java";
      }

      @Override
      String getText(final RunDescription rowData) {
        return rowData.getJavaVersion();
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Vendor";
      }

      @Override
      String getText(final RunDescription rowData) {
        return rowData.getJavaVendor();
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "OS";
      }

      @Override
      String getText(final RunDescription rowData) {
        return rowData.getOSName() + " (" + rowData.getOSVersion() + ") on " + rowData.getOSArch();
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Max Memory (MB)";
      }

      @Override
      Justification getColumnJustification() {
        return Justification.RIGHT;
      }

      @Override
      String getText(final RunDescription rowData) {
        return Integer.toString(rowData.getMaxMemoryMb());
      }

      private final Comparator<RunDescription> f_defaultComparator = new Comparator<RunDescription>() {
        @Override
        public int compare(final RunDescription o1, final RunDescription o2) {
          return o1.getMaxMemoryMb() - o2.getMaxMemoryMb();
        }
      };

      @Override
      Comparator<RunDescription> getColumnComparator() {
        return f_defaultComparator;
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Processors";
      }

      @Override
      Justification getColumnJustification() {
        return Justification.RIGHT;
      }

      @Override
      String getText(final RunDescription rowData) {
        return Integer.toString(rowData.getProcessors());
      }

      private final Comparator<RunDescription> f_defaultComparator = new Comparator<RunDescription>() {
        @Override
        public int compare(final RunDescription o1, final RunDescription o2) {
          return o1.getProcessors() - o2.getProcessors();
        }
      };

      @Override
      Comparator<RunDescription> getColumnComparator() {
        return f_defaultComparator;
      }
    });
  }
}
