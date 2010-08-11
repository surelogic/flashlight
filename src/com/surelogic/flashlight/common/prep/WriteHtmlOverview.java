package com.surelogic.flashlight.common.prep;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import com.surelogic.common.FileUtility;
import com.surelogic.common.ImageWriter;
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
import com.surelogic.flashlight.common.prep.SummaryInfo.Lock;

public final class WriteHtmlOverview implements IPostPrep {

	private static final int TABLE_LIMIT = 10;

	private static final String IMG_PACKAGE = "package.gif";
	private static final String IMG_CLASS = "class.gif";
	private static final String ALT_CLASS = "class";
	private static final String ALT_PACKAGE = "package";
	private static final String DATE_FORMAT = "yyyy.MM.dd-'at'-HH.mm.ss.SSS";
	private static final String EMPTY_LOCK_SET_INSTANCES_QUERY = "5224d411-6ed3-432f-8b91-a1c43df22911";
	private static final String LOCK_CONTENTION_QUERY = "cb38a427-c259-4690-abe2-acea9661f737";
	private static final String LOCK_EDGE_QUERY = "fd49f015-3585-4602-a5d1-4e67ef7b6a55";
	private static final String STATIC_LOCK_FREQUENCY_QUERY = "6a39e6ca-29e9-4093-ba03-0e9bd9503a1a";
	private static final String THREAD_BLOCKING_QUERY = "4e026769-1b0b-42bd-8893-f3b92add093f";
	private final RunDescription f_runDescription;
	private final StringBuilder b;
	private final ImageWriter writer;
	private final File htmlDirectory;

	public WriteHtmlOverview(final RunDescription runDescription) {
		if (runDescription == null) {
			throw new IllegalArgumentException(I18N.err(44, "runDescription"));
		}
		f_runDescription = runDescription;
		b = new StringBuilder();
		htmlDirectory = f_runDescription.getRunDirectory().getHtmlHandles()
				.getHtmlDirectory();
		writer = new ImageWriter(htmlDirectory);
	}

	public void doPostPrep(final Connection c, final SLProgressMonitor mon)
			throws SQLException {
		mon.begin();
		try {
			SummaryInfo info = new SummaryInfo.SummaryQuery()
					.perform(new ConnectionQuery(c));
			final String styleSheet = loadStyleSheet();
			final String javaScript = loadJavaScript();
			SimpleHTMLPrinter.addPageProlog(b, styleSheet, javaScript,
					"outlineInit()");
			/*
			 * TODO Add real page information/generation/read from a file here.
			 */
			b.append("<h1>").append(f_runDescription.getName()).append(' ');
			b.append(SLUtility.toStringHMS(f_runDescription.getStartTimeOfRun()));
			b.append("</h1>");
			beginTable("Vendor", "Version", "OS", "Max Memory", "Processors",
					"Start Time");
			String os = String.format("%s (%s) on %s",
					f_runDescription.getOSName(),
					f_runDescription.getOSVersion(),
					f_runDescription.getOSArch());
			row(f_runDescription.getJavaVendor(),
					f_runDescription.getJavaVersion(), os,
					f_runDescription.getMaxMemoryMb(),
					f_runDescription.getProcessors(), new SimpleDateFormat(
							DATE_FORMAT).format(f_runDescription
							.getStartTimeOfRun()));
			endTable();
			b.append("<dl>");
			def("# Observed Threads", info.getThreadCount());
			beginTable("Thread", "Time Spent Blocked");
			int count = 0;
			int threadCount = info.getThreads().size();
			for (SummaryInfo.Thread thread : info.getThreads()) {
				row(thread.getName(), thread.getBlockTime() + " ns");
				if (++count == TABLE_LIMIT) {
					break;
				}
			}
			endTable();
			if (threadCount > TABLE_LIMIT) {
				b.append(link(
						String.format("%d more results.", threadCount
								- TABLE_LIMIT), THREAD_BLOCKING_QUERY));

			}
			def("# Observed Classes", info.getClassCount());
			def("# Observed Objects", info.getObjectCount());
			dt("Potential Deadlocks");
			b.append("<script type=\"text/javascript+protovis\">"
					+ "var deadlocks = {"
					+ "  nodes:["
					+ "    {nodeName:\"Object-42\", group:1},"
					+ "    {nodeName:\"ReentrantReadWriteLock-51\", group:1}],"
					+ "  links:["
					+ "    {source:0, target:1, value: 242},"
					+ "    {source:1, target:0, value: 164}]"
					+ "};"
					+ "var vis = new pv.Panel()"
					+ "    .width(880)"
					+ "    .height(310)"
					+ "    .bottom(90);"
					+ ""
					+ "var arc = vis.add(pv.Layout.Arc)"
					+ "    .nodes(deadlocks.nodes)"
					+ "    .links(deadlocks.links)"
					+ "    .sort(function(a, b) a.group == b.group"
					+ "        ? b.linkDegree - a.linkDegree"
					+ "        : b.group - a.group);"
					+ ""
					+ "arc.link.add(pv.Line);"
					+ ""
					+ "arc.node.add(pv.Dot)"
					+ "    .size(function(d) d.linkDegree + 4)"
					+ "    .fillStyle(pv.Colors.category19().by(function(d) d.group))"
					+ "    .strokeStyle(function() this.fillStyle().darker());"
					+ "" + "arc.label.add(pv.Label);" + "" + "vis.render();"
					+ "" + "</script>");
			beginTable("Lock Held", "Lock Acquired", "Count", "First Time",
					"Last Time");
			for (Cycle cycle : info.getCycles()) {
				for (Edge e : cycle.getEdges()) {
					row(link(e.getHeld(),
							"4fce8390-7307-45dc-b779-5701ee7f23a1", "LockHeld",
							e.getHeldId(), "LockAcquired", e.getAcquiredId()),
							e.getAcquired(), e.getCount(), e.getFirst(),
							e.getLast());
				}
			}
			endTable();
			dt("Fields With No Lock Set");
			b.append("<dd>");
			displayLockSet(info);
			b.append("</dd>");
			dt("Observed Locks");
			beginTable("Lock", "Times Acquired", "Total Block Time",
					"Average Block Time");
			List<Lock> locks = info.getLocks();
			count = 0;
			for (SummaryInfo.Lock lock : locks) {
				row(link(lock.getName(), LOCK_EDGE_QUERY, "Lock", lock.getId()),
						lock.getAcquired(), lock.getBlockTime(),
						lock.getAverageBlock());
				if (++count == TABLE_LIMIT) {
					break;
				}
			}
			endTable();
			if (locks.size() > TABLE_LIMIT) {
				b.append(link(
						String.format("%d more results.", locks.size()
								- TABLE_LIMIT), LOCK_CONTENTION_QUERY));
			}
			b.append("</dl>");
			SimpleHTMLPrinter.addPageEpilog(b);

			final HtmlHandles html = f_runDescription.getRunDirectory()
					.getHtmlHandles();
			html.writeIndexHtml(b.toString());
			writer.addImage("package.gif");
			writer.addImage("class.gif");
			writer.addImage("arrow_right.gif");
			writer.addImage("arrow_down.gif");
			writer.writeImages();
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
			b.append("<ul class=\"outline\"><li>");
			b.append(writer.imageTag(IMG_PACKAGE, ALT_PACKAGE));
			b.append(pakkage);
			b.append("<ul><li>");
			b.append(writer.imageTag(IMG_CLASS, ALT_CLASS));
			b.append(clazz);
			b.append("<ul><li>");
			b.append(fieldLink(field));
			b.append("</li>");
			while (fields.hasNext()) {
				field = fields.next();
				if (!pakkage.equals(field.getPackage())) {
					pakkage = field.getPackage();
					clazz = field.getClazz();
					b.append("</ul></li></ul></li><li>");
					b.append(writer.imageTag(IMG_PACKAGE, ALT_PACKAGE));
					b.append(pakkage);
					b.append("<ul><li>");
					b.append(writer.imageTag(IMG_CLASS, ALT_CLASS));
					b.append(clazz);
					b.append("<ul>");
				} else if (!clazz.equals(field.getClazz())) {
					clazz = field.getClazz();
					b.append("</ul></li><li>");
					b.append(writer.imageTag(IMG_CLASS, ALT_CLASS));
					b.append(clazz);
					b.append("<ul>");
				}
				b.append("<li>");
				b.append(fieldLink(field));
				b.append("</li>");
			}
			b.append("</ul></li></ul></li></ul>");
		} else {
			b.append("There are no fields accessed concurrently with an empty lock set in this run.");
		}

	}

	private String fieldLink(final Field field) {
		return link(field.getName(),
				field.isStatic() ? STATIC_LOCK_FREQUENCY_QUERY
						: EMPTY_LOCK_SET_INSTANCES_QUERY, "Package",
				field.getPackage(), "Class", field.getClazz(), "Field",
				field.getName(), "FieldId", field.getId());
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
				reader = new BufferedReader(new InputStreamReader(
						styleSheetURL.openStream()));
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

	/**
	 * Loads and returns the outline JavaScript script.
	 * 
	 * @return the style sheet, or <code>null</code> if unable to load
	 */
	private String loadJavaScript() {
		final ClassLoader loader = Thread.currentThread()
				.getContextClassLoader();
		final URL protoURL = loader
				.getResource("/com/surelogic/common/js/protovis-r3.2.js");
		FileUtility.copy(protoURL, new File(htmlDirectory, "protovis-r3.2.js"));
		final URL javaScriptURL = loader
				.getResource("/com/surelogic/common/js/outline.js");
		if (javaScriptURL != null) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(
						javaScriptURL.openStream()));
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
