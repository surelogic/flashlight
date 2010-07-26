package com.surelogic.flashlight.common.prep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import com.surelogic.common.SLUtility;
import com.surelogic.common.html.SimpleHTMLPrinter;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.flashlight.common.files.HtmlHandles;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.prep.SummaryInfo.Cycle;
import com.surelogic.flashlight.common.prep.SummaryInfo.Edge;
import com.surelogic.flashlight.common.prep.SummaryInfo.Field;

public final class WriteHtmlOverview implements IPostPrep {
	private static final String DATE_FORMAT = "yyyy.MM.dd-'at'-HH.mm.ss.SSS";
	private final RunDescription f_runDescription;
	private final StringBuilder b;

	public WriteHtmlOverview(final RunDescription runDescription) {
		if (runDescription == null) {
			throw new IllegalArgumentException(I18N.err(44, "runDescription"));
		}
		f_runDescription = runDescription;
		b = new StringBuilder();
	}

	public void doPostPrep(final Connection c, final SLProgressMonitor mon)
			throws SQLException {
		mon.begin();
		try {
			SummaryInfo info = new SummaryInfo.SummaryQuery()
					.perform(new ConnectionQuery(c));
			final String styleSheet = loadStyleSheet();
			SimpleHTMLPrinter.addPageProlog(b, styleSheet);
			/*
			 * TODO Add real page information/generation/read from a file here.
			 */
			b.append("<h1>").append(f_runDescription.getName()).append(' ');
			b.append(SLUtility
					.toStringHMS(f_runDescription.getStartTimeOfRun()));
			b.append("</h1>");
			b.append("<p></p>");
			b.append("<dl>");
			def("Vendor: ", f_runDescription.getJavaVendor());
			def("Version: ", f_runDescription.getJavaVersion());
			def("OS", String.format("%s (%s) on %s", f_runDescription
					.getOSName(), f_runDescription.getOSVersion(),
					f_runDescription.getOSArch()));
			def("Max Memory", Integer.toString(f_runDescription
					.getMaxMemoryMb()));
			def("Processors", Integer
					.toString(f_runDescription.getProcessors()));
			def("Start Time", new SimpleDateFormat(DATE_FORMAT)
					.format(f_runDescription.getStartTimeOfRun()));
			def("# Observed Threads", info.getThreadCount());
			beginTable("Thread", "Time Spent Blocked");
			for (SummaryInfo.Thread thread : info.getThreads()) {
				row(thread.getName(), thread.getBlockTime());
			}
			endTable();
			def("# Observed Classes", info.getClassCount());
			def("# Observed Objects", info.getObjectCount());
			dd("Empty Lock Sets");

			b.append("</dl>");
			b
					.append("There are <a href=\"index.html?query=bd72a5e4-42aa-415d-aa72-28d351f629a4\">shared instance fields</a>.");
			beginTable("Lock Held", "Lock Acquired", "Count", "First Time",
					"Last Time");
			List<Cycle> cycles = info.getCycles();
			for (Cycle cycle : info.getCycles()) {
				for (Edge e : cycle.getEdges()) {
					row(link(e.getHeld(),
							"4fce8390-7307-45dc-b779-5701ee7f23a1", "LockHeld",
							e.getHeldId(), "LockAcquired", e.getAcquiredId()),
							e.getAcquired(), e.getCount(), e.getFirst(), e
									.getLast());
				}
			}
			endTable();
			dt("Fields With No Lock Set");
			b.append("<dd>");
			displayLockSet(info);
			b.append("</dd>");
			beginTable("Lock", "Times Acquired", "Total Block Time",
					"Average Block Time");
			for (SummaryInfo.Lock lock : info.getLocks()) {
				row(link(lock.getName(),
						"fd49f015-3585-4602-a5d1-4e67ef7b6a55", "Lock", lock
								.getId()), lock.getAcquired(), lock
						.getBlockTime(), lock.getAverageBlock());
			}
			endTable();
			SimpleHTMLPrinter.addPageEpilog(b);

			final HtmlHandles html = f_runDescription.getRunDirectory()
					.getHtmlHandles();
			html.writeIndexHtml(b.toString());
		} finally {
			mon.done();
		}
	}

	private void displayLockSet(final SummaryInfo info) {
		Iterator<Field> fields = info.getEmptyLockSetFields().iterator();
		if (fields.hasNext()) {
			Field field = fields.next();
			String pakkage = field.getPackage();
			String clazz = field.getClazz();
			b.append("<dl><dt>");
			b.append(pakkage);
			b.append("</dt><dd><dl><dt>");
			b.append(clazz);
			b.append("</dt><dd><ul><li>");
			b.append(field.getName());
			b.append("</li>");
			while (fields.hasNext()) {
				field = fields.next();
				if (!pakkage.equals(field.getPackage())) {
					b.append("</ul></dd></dd><dt>");
					b.append(pakkage);
					b.append("</dt><dd><dl><dt>");
					b.append(clazz);
					b.append("</dt><dd><ul><li>");
				} else if (!clazz.equals(field.getClazz())) {
					b.append("</ul></dd><dt>");
					b.append(clazz);
					b.append("</dt><dd><ul>");
				}
				b.append("<li>");
				b.append(field.getName());
				b.append("</li>");
			}
			b.append("</ul></dd></dl></dd></dl>");
		} else {
			b
					.append("There are no fields accessed concurrently with an empty lock set in this run.");
		}

	}

	private WriteHtmlOverview def(final String term, final String definition) {
		b.append("<dt>");
		b.append(term);
		b.append("</dt>");
		b.append("<dd>");
		b.append(definition);
		b.append("</dd>");
		return this;
	}

	private WriteHtmlOverview dd(final String definition) {
		b.append("<dd>");
		b.append(definition);
		b.append("</dd>");
		return this;
	}

	private WriteHtmlOverview dt(final String term) {
		b.append("<dt>");
		b.append(term);
		b.append("</dt>");
		return this;
	}

	private WriteHtmlOverview beginTable(final String... columns) {
		b.append("<table>");
		if (columns.length > 0) {
			b.append("<tr>");
			for (String name : columns) {
				b.append("<th>");
				b.append(name);
				b.append("</th>");
			}
			b.append("</tr>");
		}
		return this;
	}

	private WriteHtmlOverview endTable() {
		b.append("</table>");
		return this;
	}

	private WriteHtmlOverview row(final Object... columns) {
		b.append("<tr>");
		for (Object col : columns) {
			b.append("<td>");
			b.append(col);
			b.append("</td>");
		}
		b.append("</tr>");
		return this;
	}

	/**
	 * Creates a link with the given params. Parameters are based in order.
	 * First name, then value. For example:
	 * <code>link(name, "bd72", "name", "foo")</code> would produce a link
	 * pointing to <code>index.html?query=bd72&name=foo</code>
	 * 
	 * @param args
	 * @return
	 */
	private static String link(final String text, final String query,
			final String... args) {
		StringBuilder b = new StringBuilder();
		if (args.length % 2 != 0) {
			throw new IllegalArgumentException(
					"There must be an even number of arguments");
		}
		b.append("<a href=\"index.html?query=");
		b.append(query);
		for (int i = 0; i < args.length; i = i + 2) {
			b.append('&');
			b.append(args[i]);
			b.append('=');
			b.append(args[i + 1]);
		}
		b.append("\">");
		b.append(text);
		b.append("</a>");
		return b.toString();
	}

	public String getDescription() {
		return "Generating an HTML overview of the results of this run";
	}

	/**
	 * Loads and returns the Javadoc hover style sheet.
	 * 
	 * @return the style sheet, or <code>null</code> if unable to load
	 */
	private static String loadStyleSheet() {
		final URL styleSheetURL = Thread
				.currentThread()
				.getContextClassLoader()
				.getResource(
						"/com/surelogic/flashlight/common/prep/RunOverviewStyleSheet.css");
		if (styleSheetURL != null) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(styleSheetURL
						.openStream()));
				final StringBuffer buffer = new StringBuffer(1500);
				String line = reader.readLine();
				while (line != null) {
					buffer.append(line);
					buffer.append('\n');
					line = reader.readLine();
				}
				return buffer.toString();
			} catch (final IOException ex) {
				throw new IllegalStateException(ex);
			} finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (final IOException e) {
				}
			}
		}
		return null;
	}
}
