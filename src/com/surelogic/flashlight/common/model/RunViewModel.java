package com.surelogic.flashlight.common.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.surelogic.common.Justification;
import com.surelogic.common.images.CommonImages;
import com.surelogic.flashlight.common.Utility;
import com.surelogic.flashlight.common.files.RawFileHandles;

/**
 * IDE independent data model for the table of Flashlight runs displayed in the
 * run view. The columns of this table are shown in the list below:
 * 
 * <ul>
 * <li>Raw</li>
 * <li>Prep</li>
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
	 *            the column index.
	 * @return the non-null title for the passed column.
	 */
	public String getColumnTitle(int column) {
		return f_columnData.get(column).getColumnTitle();
	}

	/**
	 * Gets the column justification for the passed column.
	 * 
	 * @param column
	 *            the column index.
	 * @return the column justification for the passed column.
	 */
	public Justification getColumnJustification(int column) {
		return f_columnData.get(column).getColumnJustification();
	}

	/**
	 * Gets the text for the passed row and column.
	 * 
	 * @param rowData
	 *            the data for the row.
	 * @param column
	 *            the column index.
	 * @return the non-null text for the passed row and column.
	 */
	public String getText(RunDescription rowData, int column) {
		return f_columnData.get(column).getText(rowData);
	}

	/**
	 * Gets the symbolic name from {@link CommonImages} or {@code null} if no
	 * image should be displayed for the passed row data and column.
	 * 
	 * @param rowData
	 *            the data for the row.
	 * @param column
	 *            the column index.
	 * @return a symbolic name from {@link CommonImages} or {@code null} if no
	 *         image should be displayed.
	 */
	public String getImageSymbolicName(RunDescription rowData, int column) {
		return f_columnData.get(column).getImageSymbolicName(rowData);
	}

	/**
	 * Gets the {@link Comparator} used for sorting the passed column.
	 * 
	 * @param column
	 *            the column index.
	 * @return the non-null {@link Comparator} used for sorting the passed
	 *         column.
	 */
	public Comparator<RunDescription> getColumnComparator(int column) {
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

		String getText(RunDescription rowData) {
			return "";
		}

		String getImageSymbolicName(RunDescription rowData) {
			return null;
		}

		private final Comparator<RunDescription> f_defaultComparator = new Comparator<RunDescription>() {
			public int compare(RunDescription o1, RunDescription o2) {
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
				return "Raw";
			}

			@Override
			String getText(RunDescription rowData) {
				final RawFileHandles handles = rowData.getRawFileHandles();
				if (handles != null) {
					final long sizeKb = handles.getDataFile().length() / 1024;
					String dataFileSizeText;
					if (sizeKb < 1024) {
						dataFileSizeText = "(" + sizeKb + " KB)";
					} else {
						dataFileSizeText = "(" + (sizeKb / 1024) + " MB)";
					}
					return dataFileSizeText;
				}
				return super.getText(rowData);
			}

			@Override
			String getImageSymbolicName(RunDescription rowData) {
				if (rowData.getRawFileHandles() != null) {
					return CommonImages.IMG_FILE;
				}
				return super.getImageSymbolicName(rowData);
			}
		});

		f_columnData.add(new ColumnDataAdaptor() {
			@Override
			String getColumnTitle() {
				return "Prep";
			}

			@Override
			String getImageSymbolicName(RunDescription rowData) {
				if (rowData.getPrepRunDescription() != null) {
					return CommonImages.IMG_DRUM;
				}
				return super.getImageSymbolicName(rowData);
			}

			private final Comparator<RunDescription> f_defaultComparator = new Comparator<RunDescription>() {
				public int compare(RunDescription o1, RunDescription o2) {
					final int i1 = o1.getPrepRunDescription() != null ? 0 : 1;
					final int i2 = o2.getPrepRunDescription() != null ? 0 : 1;
					return i1 - i2;
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
			String getText(RunDescription rowData) {
				return rowData.getName();
			}

			@Override
			String getImageSymbolicName(RunDescription rowData) {
				RawFileHandles handles = rowData.getRawFileHandles();
				if (handles != null) {
					if (!handles.isLogClean()) {
						return CommonImages.IMG_WARNING;
					}
				}
				return CommonImages.IMG_FL_RUN_OBJ;
			}
		});

		f_columnData.add(new ColumnDataAdaptor() {
			@Override
			String getColumnTitle() {
				return "Time";
			}

			@Override
			String getText(RunDescription rowData) {
				return Utility.toStringMS(rowData.getStartTimeOfRun());
			}
		});

		f_columnData.add(new ColumnDataAdaptor() {
			@Override
			String getColumnTitle() {
				return "By";
			}

			@Override
			String getText(RunDescription rowData) {
				return rowData.getUserName();
			}
		});

		f_columnData.add(new ColumnDataAdaptor() {
			@Override
			String getColumnTitle() {
				return "Java";
			}

			@Override
			String getText(RunDescription rowData) {
				return rowData.getJavaVersion();
			}
		});

		f_columnData.add(new ColumnDataAdaptor() {
			@Override
			String getColumnTitle() {
				return "Vendor";
			}

			@Override
			String getText(RunDescription rowData) {
				return rowData.getJavaVendor();
			}
		});

		f_columnData.add(new ColumnDataAdaptor() {
			@Override
			String getColumnTitle() {
				return "OS";
			}

			@Override
			String getText(RunDescription rowData) {
				return rowData.getOSName() + " (" + rowData.getOSVersion()
						+ ") on " + rowData.getOSArch();
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
			String getText(RunDescription rowData) {
				return Integer.toString(rowData.getMaxMemoryMb());
			}

			private final Comparator<RunDescription> f_defaultComparator = new Comparator<RunDescription>() {
				public int compare(RunDescription o1, RunDescription o2) {
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
			String getText(RunDescription rowData) {
				return Integer.toString(rowData.getProcessors());
			}

			private final Comparator<RunDescription> f_defaultComparator = new Comparator<RunDescription>() {
				public int compare(RunDescription o1, RunDescription o2) {
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
