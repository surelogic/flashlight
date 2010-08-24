package com.surelogic.flashlight.common.prep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import com.surelogic.common.FileUtility;
import com.surelogic.common.ImageWriter;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.flashlight.common.files.HtmlHandles;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Body;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Container;
import com.surelogic.flashlight.common.prep.HTMLBuilder.HTMLList;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Head;
import com.surelogic.flashlight.common.prep.HTMLBuilder.LI;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Row;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Table;
import com.surelogic.flashlight.common.prep.HTMLBuilder.UL;
import com.surelogic.flashlight.common.prep.SummaryInfo.Cycle;
import com.surelogic.flashlight.common.prep.SummaryInfo.Edge;
import com.surelogic.flashlight.common.prep.SummaryInfo.Field;

public final class WriteHtmlOverview implements IPostPrep {

	private static final int TABLE_LIMIT = 10;

	private static final String DATE_FORMAT = "yyyy.MM.dd-'at'-HH.mm.ss.SSS";
	private static final String EMPTY_LOCK_SET_INSTANCES_QUERY = "5224d411-6ed3-432f-8b91-a1c43df22911";
	private static final String LOCK_CONTENTION_QUERY = "cb38a427-c259-4690-abe2-acea9661f737";
	private static final String LOCK_EDGE_QUERY = "fd49f015-3585-4602-a5d1-4e67ef7b6a55";
	private static final String STATIC_LOCK_FREQUENCY_QUERY = "6a39e6ca-29e9-4093-ba03-0e9bd9503a1a";
	private static final String THREAD_BLOCKING_QUERY = "4e026769-1b0b-42bd-8893-f3b92add093f";
	private final RunDescription f_runDescription;
	private final ImageWriter writer;
	private final File htmlDirectory;

	public WriteHtmlOverview(final RunDescription runDescription) {
		if (runDescription == null) {
			throw new IllegalArgumentException(I18N.err(44, "runDescription"));
		}
		f_runDescription = runDescription;
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

			HTMLBuilder builder = new HTMLBuilder();
			Head head = builder.head(f_runDescription.getName());
			loadStyleSheet(head);
			loadJavaScript(head);
			Body body = builder.body();
			body.div().id("header").h(1).text(f_runDescription.getName());

			Container main = body.div().id("main");
			Container sidebar = main.div().id("bar");
			sidebar.h(2).a("#locks").text("Locks");
			sidebar.h(2).a("#fields").text("Fields");
			sidebar.h(2).a("#threads").text("Threads");
			Container content = main.div().id("content");
			Table runTable = content.table().id("run-table");
			runTable.header().th("Vendor").th("Version").th("OS")
					.th("Max Memory").th("Processors").th("Start Time");
			String os = String.format("%s (%s) on %s",
					f_runDescription.getOSName(),
					f_runDescription.getOSVersion(),
					f_runDescription.getOSArch());
			runTable.row()
					.td(f_runDescription.getJavaVendor())
					.td(f_runDescription.getJavaVersion())
					.td(os)
					.td(Integer.toString(f_runDescription.getMaxMemoryMb()))
					.td(Integer.toString(f_runDescription.getProcessors()))
					.td(new SimpleDateFormat(DATE_FORMAT)
							.format(f_runDescription.getStartTimeOfRun()));

			Container lockDiv = content.div().id("locks");
			lockDiv.h(2).text("Locks");
			lockDiv.h(3).text("Lock Contention");

			Table lockTable = lockDiv.table();
			lockTable.header().th("Lock").th("Times Acquired")
					.th("Total Block Time").th("AverageBlockTime");

			List<SummaryInfo.Lock> locks = info.getLocks();
			int count = 0;
			for (SummaryInfo.Lock lock : locks) {
				Row r = lockTable.row();
				link(r.td(), lock.getName(), LOCK_EDGE_QUERY, "Lock",
						lock.getId());
				r.td(Integer.toString(lock.getAcquired()))
						.td(Long.toString(lock.getBlockTime()) + " ns")
						.td(Long.toString(lock.getAverageBlock()) + " ns");
				if (++count == TABLE_LIMIT) {
					break;
				}
			}
			if (locks.size() > TABLE_LIMIT) {
				link(lockDiv,
						String.format("%d more results.", locks.size()
								- TABLE_LIMIT), LOCK_CONTENTION_QUERY);
			}

			lockDiv.h(3).text("Potential Deadlocks");
			HTMLList deadlockList = lockDiv.ul().id("deadlock-list");
			List<Cycle> cycles = info.getCycles();
			for (Cycle cycle : cycles) {
				deadlockList.li().id("cycle" + cycle.getNum())
						.text(String.format("Cycle %d", cycle.getNum()));
			}
			writeCycles(cycles);
			lockDiv.div().id("deadlock-widget");
			// beginTable("Lock Held", "Lock Acquired", "Count", "First Time",
			// "Last Time");
			// for (Cycle cycle : info.getCycles()) {
			// for (Edge e : cycle.getEdges()) {
			// row(link(e.getHeld(),
			// "4fce8390-7307-45dc-b779-5701ee7f23a1", "LockHeld",
			// e.getHeldId(), "LockAcquired", e.getAcquiredId()),
			// e.getAcquired(), e.getCount(), e.getFirst(),
			// e.getLast());
			// }
			// }
			// endTable();

			Container fieldDiv = content.div().id("fields");
			fieldDiv.h(2).text("Fields");
			fieldDiv.h(3).text("Shared Fields With No Lock Set");
			displayLockSet(info, fieldDiv);
			Container threadDiv = content.div().id("threads");
			threadDiv.h(2).text("Threads");
			int threadCount = info.getThreads().size();
			threadDiv.p(String.format(
					"%d threads were observed in this run of the program.",
					threadCount));
			threadDiv.h(3).text("Thread Liveness");

			Table threadTable = threadDiv.table();
			threadTable.header().th("Thread").th("Time Blocked");
			count = 0;
			for (SummaryInfo.Thread thread : info.getThreads()) {
				threadTable.row().td(thread.getName())
						.td(thread.getBlockTime() + " ns");
				if (++count == TABLE_LIMIT) {
					break;
				}
			}
			if (threadCount > TABLE_LIMIT) {
				link(threadDiv,
						String.format("%d more results.", threadCount
								- TABLE_LIMIT), THREAD_BLOCKING_QUERY);
			}
			// def("# Observed Classes", info.getClassCount());
			// def("# Observed Objects", info.getObjectCount());

			final HtmlHandles html = f_runDescription.getRunDirectory()
					.getHtmlHandles();
			html.writeIndexHtml(builder.build());
			writer.addImage("package.gif");
			writer.addImage("class.gif");
			writer.addImage("arrow_right.gif");
			writer.addImage("arrow_down.gif");
			writer.writeImages();
		} finally {
			mon.done();
		}
	}

	private void writeCycles(final List<Cycle> cycles) {
		try {
			PrintWriter writer = new PrintWriter(new File(htmlDirectory,
					"cycles-data.js"));
			writer.println("var deadlocks = {");
			for (Iterator<Cycle> ci = cycles.iterator(); ci.hasNext();) {
				Cycle c = ci.next();
				writer.printf("\tcycle%d : [", c.getNum());
				for (Iterator<Edge> ei = c.getEdges().iterator(); ei.hasNext();) {
					Edge e = ei.next();
					writer.println("{");
					writer.printf("\t\t\"id\": \"%s\",\n", e.getHeld());
					writer.printf("\t\t\"name\": \"%s\",\n", e.getHeld());
					writer.printf(
							"\t\t\"adjacencies\": [{\"nodeTo\": \"%2$s\", \"data\": { \"$type\": \"arrow\", \"$direction\": [\"%1$s\",\"%2$s\"]}}]\n",
							e.getHeld(), e.getAcquired());
					writer.println("}");
					if (ei.hasNext()) {
						writer.print(", ");
					}
				}
				writer.println("]");
				if (ci.hasNext()) {
					writer.print(", ");
				}
			}
			writer.println("};");
			writer.close();
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private void displayLockSet(final SummaryInfo info, final Container c) {
		UL packageList = c.ul();

		Iterator<Field> fields = info.getEmptyLockSetFields().iterator();
		if (fields.hasNext()) {
			Field field = fields.next();
			String pakkage = field.getPackage();
			String clazz = field.getClazz();
			LI packageLI = packageList.li();
			packageLI.h(4).text(pakkage);
			UL classList = packageLI.ul();
			LI classLI = classList.li();
			classLI.h(4).text(clazz);
			UL fieldList = classLI.ul();
			LI fieldLI = fieldList.li();
			fieldLink(fieldLI, field);
			while (fields.hasNext()) {
				field = fields.next();
				if (!pakkage.equals(field.getPackage())) {
					pakkage = field.getPackage();
					clazz = field.getClazz();
					packageLI = packageList.li();
					packageLI.h(4).text(pakkage);
					classList = packageLI.ul();
					classLI = classList.li();
					classLI.h(4).text(clazz);
					fieldList = classLI.ul();
				} else if (!clazz.equals(field.getClazz())) {
					clazz = field.getClazz();
					classLI = classList.li();
					classLI.h(4).text(clazz);
					fieldList = classLI.ul();
				}
				fieldLI = fieldList.li();
				fieldLink(fieldLI, field);
			}
		} else {
			c.p("There are no fields accessed concurrently with an empty lock set in this run.");
		}

	}

	private void fieldLink(final Container c, final Field field) {
		link(c, field.getName(), field.isStatic() ? STATIC_LOCK_FREQUENCY_QUERY
				: EMPTY_LOCK_SET_INSTANCES_QUERY, "Package",
				field.getPackage(), "Class", field.getClazz(), "Field",
				field.getName(), "FieldId", field.getId());
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
	private static void link(final Container c, final String text,
			final String query, final String... args) {
		StringBuilder b = new StringBuilder();
		if (args.length % 2 != 0) {
			throw new IllegalArgumentException(
					"There must be an even number of arguments");
		}
		b.append("index.html?query=");
		b.append(query);
		for (int i = 0; i < args.length; i = i + 2) {
			b.append('&');
			b.append(args[i]);
			b.append('=');
			b.append(args[i + 1]);
		}
		c.a(b.toString()).text(text);
	}

	public String getDescription() {
		return "Generating an HTML overview of the results of this run";
	}

	/**
	 * Loads and includes the Javadoc hover style sheet.
	 * 
	 * @return the style sheet, or <code>null</code> if unable to load
	 */
	private void loadStyleSheet(final Head head) {
		final URL styleSheetURL = Thread
				.currentThread()
				.getContextClassLoader()
				.getResource(
						"/com/surelogic/flashlight/common/prep/RunOverviewStyleSheet.css");
		FileUtility.copy(styleSheetURL, new File(htmlDirectory,
				"RunOverviewStyleSheet.css"));
		head.styleSheet("RunOverviewStyleSheet.css");
	}

	/**
	 * Loads and returns the outline JavaScript script.
	 * 
	 * @return the style sheet, or <code>null</code> if unable to load
	 */
	private void loadJavaScript(final Head head) {
		final ClassLoader loader = Thread.currentThread()
				.getContextClassLoader();
		final URL vis = loader.getResource("/com/surelogic/common/js/jit.js");
		final URL jquery = loader
				.getResource("/com/surelogic/common/js/jquery-1.4.2.min.js");
		final URL canvas = loader
				.getResource("/com/surelogic/common/js/excanvas.js");
		final URL cycles = loader
				.getResource("/com/surelogic/flashlight/common/prep/cycles.js");
		FileUtility.copy(cycles, new File(htmlDirectory, "cycles.js"));
		FileUtility.copy(canvas, new File(htmlDirectory, "excanvas.js"));
		FileUtility.copy(vis, new File(htmlDirectory, "jit.js"));
		FileUtility
				.copy(jquery, new File(htmlDirectory, "jquery-1.4.2.min.js"));

		head.javaScript("excanvas.js", true);
		head.javaScript("jit.js");
		head.javaScript("jquery-1.4.2.min.js");
		head.javaScript("cycles-data.js");
		head.javaScript("cycles.js");
	}

}
