package com.surelogic.flashlight.client.eclipse.views.run;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.surelogic.common.CommonImages;
import com.surelogic.common.FileUtility;
import com.surelogic.common.Justification;
import com.surelogic.common.SLUtility;
import com.surelogic.flashlight.client.eclipse.model.RunManager;
import com.surelogic.flashlight.common.model.RawFileHandles;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunDirectory;

/**
 * IDE independent data model for the table of Flashlight runs displayed in the
 * run view. The columns of this table are shown in the list below:
 * 
 * <ul>
 * <li>Size</li>
 * <li>Run</li>
 * <li>Time</li>
 * <li>Processors</li>
 * <li>By</li>
 * <li>Java</li>
 * <li>Vendor</li>
 * <li>OS</li>
 * <li>Max Memory (MB)</li>
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
  public String getText(final RunDirectory rowData, final int column) {
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
  public String getImageSymbolicName(final RunDirectory rowData, final int column) {
    return f_columnData.get(column).getImageSymbolicName(rowData);
  }

  /**
   * Gets the {@link Comparator} used for sorting the passed column.
   * 
   * @param column
   *          the column index.
   * @return the non-null {@link Comparator} used for sorting the passed column.
   */
  public Comparator<RunDirectory> getColumnComparator(final int column) {
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

  static abstract class ColumnDataAdaptor {

    abstract String getColumnTitle();

    Justification getColumnJustification() {
      return Justification.LEFT;
    }

    String getText(final RunDirectory rowData) {
      return "";
    }

    String getImageSymbolicName(final RunDirectory rowData) {
      return null;
    }

    private final Comparator<RunDirectory> f_defaultComparator = new Comparator<RunDirectory>() {
      @Override
      public int compare(final RunDirectory o1, final RunDirectory o2) {
        return getText(o1).compareToIgnoreCase(getText(o2));
      }
    };

    Comparator<RunDirectory> getColumnComparator() {
      return f_defaultComparator;
    }
  }

  private final List<ColumnDataAdaptor> f_columnData = new ArrayList<>();

  public RunViewModel() {
    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Size";
      }

      @Override
      String getText(final RunDirectory rowData) {
        if (rowData == null || rowData.getDirectory() == null) {
          return "Unknown";
        }
        return rowData.getHumanReadableSize();
      }

      @Override
      String getImageSymbolicName(final RunDirectory rowData) {
        if (rowData.isPrepared()) {
          return CommonImages.IMG_DRUM;
        } else if (RunManager.getInstance().isBeingPrepared(rowData))
          return CommonImages.IMG_REFRESH;
        else
          return CommonImages.IMG_FILE;
      }

      private final Comparator<RunDirectory> f_defaultComparator = new Comparator<RunDirectory>() {
        @Override
        public int compare(final RunDirectory o1, final RunDirectory o2) {
          long size1 = FileUtility.recursiveSizeInBytes(o1.getDirectory());
          long size2 = FileUtility.recursiveSizeInBytes(o2.getDirectory());
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
      Comparator<RunDirectory> getColumnComparator() {
        return f_defaultComparator;
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Run";
      }

      @Override
      String getText(final RunDirectory rowData) {
        return rowData.getDescription().getSimpleName();
      }

      @Override
      String getImageSymbolicName(final RunDirectory rowData) {
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
      String getText(final RunDirectory rowData) {
        return SLUtility.toStringDayHMS(rowData.getDescription().getStartTimeOfRun());
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Duration";
      }

      @Override
      String getText(final RunDirectory rowData) {
        long duration = rowData.getDescription().getCollectionDurationInNanos();
        return SLUtility.toStringDurationMS(duration, TimeUnit.NANOSECONDS);
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Processors";
      }

      @Override
      String getImageSymbolicName(RunDirectory rowData) {
        return CommonImages.IMG_CPU_SUBDUED;
      }

      @Override
      Justification getColumnJustification() {
        return Justification.RIGHT;
      }

      @Override
      String getText(final RunDirectory rowData) {
        return SLUtility.toStringHumanWithCommas(rowData.getDescription().getProcessors());
      }

      private final Comparator<RunDirectory> f_defaultComparator = new Comparator<RunDirectory>() {
        @Override
        public int compare(final RunDirectory o1, final RunDirectory o2) {
          return o1.getDescription().getProcessors() - o2.getDescription().getProcessors();
        }
      };

      @Override
      Comparator<RunDirectory> getColumnComparator() {
        return f_defaultComparator;
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Max Memory";
      }

      @Override
      Justification getColumnJustification() {
        return Justification.RIGHT;
      }

      @Override
      String getText(final RunDirectory rowData) {
        return SLUtility.toStringHumanWithCommas(rowData.getDescription().getMaxMemoryMb()) + " MB";
      }

      private final Comparator<RunDirectory> f_defaultComparator = new Comparator<RunDirectory>() {
        @Override
        public int compare(final RunDirectory o1, final RunDirectory o2) {
          return o1.getDescription().getMaxMemoryMb() - o2.getDescription().getMaxMemoryMb();
        }
      };

      @Override
      Comparator<RunDirectory> getColumnComparator() {
        return f_defaultComparator;
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Java";
      }

      @Override
      String getText(final RunDirectory rowData) {
        return rowData.getDescription().getJavaVersion();
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Java Vendor";
      }

      @Override
      String getText(final RunDirectory rowData) {
        return rowData.getDescription().getJavaVendor();
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "OS";
      }

      @Override
      String getText(final RunDirectory rowData) {
        final RunDescription desc = rowData.getDescription();
        return desc.getOSName() + " (" + desc.getOSVersion() + ") on " + desc.getOSArch();
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "Host";
      }

      @Override
      String getText(final RunDirectory rowData) {
        return rowData.getDescription().getHostname();
      }
    });

    f_columnData.add(new ColumnDataAdaptor() {
      @Override
      String getColumnTitle() {
        return "By";
      }

      @Override
      String getText(final RunDirectory rowData) {
        return rowData.getDescription().getUserName();
      }
    });
  }
}
