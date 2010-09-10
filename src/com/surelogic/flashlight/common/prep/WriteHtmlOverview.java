package com.surelogic.flashlight.common.prep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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
	private static final String GRAPH_DATE_FORMAT = "MMM dd yyyy HH:mm:ss zz";
	private static final String EMPTY_LOCK_SET_INSTANCES_QUERY = "5224d411-6ed3-432f-8b91-a1c43df22911";
	private static final String LOCK_CONTENTION_QUERY = "cb38a427-c259-4690-abe2-acea9661f737";
	private static final String LOCK_EDGE_QUERY = "fd49f015-3585-4602-a5d1-4e67ef7b6a55";
	private static final String STATIC_LOCK_FREQUENCY_QUERY = "6a39e6ca-29e9-4093-ba03-0e9bd9503a1a";
	private static final String THREAD_BLOCKING_QUERY = "4e026769-1b0b-42bd-8893-f3b92add093f";
	private final RunDescription f_runDescription;
	private final ImageWriter writer;
	private final File htmlDirectory;
	private final Calendar c;

	public WriteHtmlOverview(final RunDescription runDescription) {
		if (runDescription == null) {
			throw new IllegalArgumentException(I18N.err(44, "runDescription"));
		}
		f_runDescription = runDescription;
		htmlDirectory = f_runDescription.getRunDirectory().getHtmlHandles()
				.getHtmlDirectory();
		writer = new ImageWriter(htmlDirectory);
		c = Calendar.getInstance();
	}

	public void doPostPrep(final Connection c, final SLProgressMonitor mon)
			throws SQLException {
		mon.begin();
		try {
			SummaryInfo info = new SummaryInfo.SummaryQuery()
					.perform(new ConnectionQuery(c));
			PrintWriter graphs;
			try {
				graphs = new PrintWriter(new File(htmlDirectory,
						"graph-data.js"));
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
			HTMLBuilder builder = new HTMLBuilder();
			Head head = builder.head(f_runDescription.getName());
			loadStyleSheet(head);
			loadTimeline(head);
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

			content.div().id("tl");
			writeThreadTimeline(graphs, info);

			Container lockDiv = content.div().id("locks");

			lockDiv.h(2).text("Locks");
			lockDiv.h(3).text("Lock Contention");

			Table lockTable = lockDiv.table();
			lockTable.header().th("Lock").th("Times Acquired")
					.th("Total Block Time").th("Average Block Time");

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
			writeCycles(graphs, cycles);
			Table dTable = lockDiv.table().id("deadlock-container");
			Row dRow = dTable.row();
			dRow.td().id("deadlock-widget");
			Container threads = dRow.td().id("deadlock-threads");

			threads.h(3).text("Foo-4");
			threads.h(3).text("Foo-5");

			Container fieldDiv = content.div().id("fields");
			fieldDiv.h(2).text("Fields");
			fieldDiv.h(3).text("Shared Fields With No Lock Set");
			fieldDiv.div().id("packages");
			fieldDiv.div().id("packages1");
			fieldDiv.div().id("packages2");
			fieldDiv.div().id("packages3");
			writeLockSet(graphs, info);
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
			graphs.close();
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

	private void writeThreadTimeline(final PrintWriter writer,
			final SummaryInfo info) {
		SimpleDateFormat df = new SimpleDateFormat(GRAPH_DATE_FORMAT);
		Date first = null, last = null;
		for (SummaryInfo.Thread t : info.getThreads()) {
			Date start = t.getStart();
			Date stop = t.getStop();
			if (first == null || start.getTime() < first.getTime()) {
				first = t.getStart();
			}
			if (last == null || last.getTime() > stop.getTime()) {
				last = t.getStop();
			}
		}

		writer.println("var timeline_data = {");
		writer.println(String.format(
				"'first': Timeline.DateTime.parseGregorianDateTime('%s'),",
				df.format(first)));
		writer.println(String.format(
				"'last': Timeline.DateTime.parseGregorianDateTime('%s'),",
				df.format(last)));
		writer.println("'dateTimeFormat': 'javascriptnative', ");
		writer.println("'events': [");
		for (SummaryInfo.Thread t : info.getThreads()) {
			Date start = t.getStart();
			Date stop = t.getStop();
			writer.print(String
					.format("{'start': %s, 'end': %s, 'title': \"%s\", 'description': 'Thread Duration: %(,.3f seconds', 'durationEvent': true, 'color': 'blue' }",
							jsDate(start), jsDate(stop), t.getName(),
							(float) (stop.getTime() - start.getTime()) / 1000));
			writer.println(",");
		}
		writer.print(String
				.format("{'start': %s, 'end': %s, 'title': \"%s\", 'description': 'Program Duration: %(,.3f seconds', 'durationEvent': true, 'color': 'red' }",
						jsDate(first), jsDate(last), "Program Run Time",
						(float) (last.getTime() - first.getTime()) / 1000));
		writer.println("]};");
	}

	private String jsDate(final Date date) {
		c.setTime(date);
		return String.format("new Date(%d,%d,%d,%d,%d,%d, %d)",
				c.get(Calendar.YEAR), c.get(Calendar.MONTH),
				c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY),
				c.get(Calendar.MINUTE), c.get(Calendar.SECOND),
				c.get(Calendar.MILLISECOND));
	}

	/**
	 * A little helper class to find 2-lock cycles and mark them differently in
	 * the graph.
	 * 
	 * @author nathan
	 * 
	 */
	private static class EdgeEntry implements Comparable<EdgeEntry> {
		final String from;
		final String to;

		EdgeEntry(final String from, final String to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (from == null ? 0 : from.hashCode());
			result = prime * result + (to == null ? 0 : to.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			EdgeEntry other = (EdgeEntry) obj;
			if (from == null) {
				if (other.from != null) {
					return false;
				}
			} else if (!from.equals(other.from)) {
				return false;
			}
			if (to == null) {
				if (other.to != null) {
					return false;
				}
			} else if (!to.equals(other.to)) {
				return false;
			}
			return true;
		}

		public int compareTo(final EdgeEntry o) {
			int cmp = from.compareTo(o.from);
			if (cmp == 0) {
				cmp = to.compareTo(o.to);
			}
			return cmp;
		}

		@Override
		public String toString() {
			return "EdgeEntry [from=" + from + ", to=" + to + "]";
		}

	}

	private static class PackageFrag {
		final String frag;
		final Map<String, PackageFrag> frags = new TreeMap<String, PackageFrag>();
		final List<Field> fields = new ArrayList<Field>();

		PackageFrag(final String f) {
			this.frag = f;
		}

		PackageFrag frag(final String f) {
			PackageFrag frag = frags.get(f);
			if (frag == null) {
				frag = new PackageFrag(f);
				frags.put(f, frag);
			}
			return frag;
		}

		PackageFrag field(final Field f) {
			fields.add(f);
			return this;
		}

		private static final int AREA = 200;

		// Calculates and caches the area
		private int area() {
			return fields.size() + AREA;
		}

		String buildNode(final String id, final String pakkage) {
			StringBuilder b = new StringBuilder();
			String newPackage = pakkage + "." + frag;
			if (!fields.isEmpty()) {
				b.append("{");
				b.append(String
						.format("\"id\": \"%s\",", id + "." + newPackage));
				b.append(String.format("\"name\": \"%s\",", frag));
				b.append(String.format("\"data\": {\"$area\": \"%d\" },",
						area()));
				b.append("\"children\": [");
				for (Iterator<Field> iter = fields.iterator(); iter.hasNext();) {
					Field f = iter.next();
					b.append("{");
					b.append(String.format("\"id\": \"%s\",", id + "."
							+ newPackage + f.getName()));
					b.append(String.format("\"name\": \"%s\",", f.getName()));
					b.append(String
							.format("\"data\": { \"$area\": %d, \"$color\": \"#1165f1\" }",
									AREA));
					b.append("}");
					if (iter.hasNext()) {
						b.append(",");
					}
				}
				b.append("]");
				b.append("}");
			}
			boolean firstComma = fields.isEmpty();
			for (PackageFrag frag : frags.values()) {
				String node = frag.buildNode(id, newPackage);
				if (!node.isEmpty()) {
					if (firstComma) {
						firstComma = false;
					} else {
						b.append(",");
					}
					b.append(node);
				}
			}
			return b.toString();
		}

		String buildNode(final String parentId) {
			StringBuilder b = new StringBuilder();
			b.append("{");
			b.append(String.format("\"id\": \"%s\",", parentId + "." + frag));
			b.append(String.format("\"name\": \"%s\",", frag));
			b.append("\"children\": [");
			for (Iterator<Field> iter = fields.iterator(); iter.hasNext();) {
				Field f = iter.next();
				b.append("{");
				b.append(String.format("\"id\": \"%s\",", parentId + "." + frag
						+ f.getName()));
				b.append(String.format("\"name\": \"%s\",", f.getName()));
				b.append("\"data\": { \"$area\": 200, \"$color\": \"#1165f1\" }");
				b.append("}");
				if (iter.hasNext()) {
					b.append(",");
				}
			}
			if (!fields.isEmpty() && !frags.isEmpty()) {
				b.append(",");
			}
			for (Iterator<PackageFrag> iter = frags.values().iterator(); iter
					.hasNext();) {
				PackageFrag child = iter.next();
				b.append(child.buildNode(parentId + "." + frag));
				if (iter.hasNext()) {
					b.append(",");
				}
			}
			b.append("]");
			b.append("}");
			return b.toString();
		}

		@Override
		public String toString() {
			return buildNode("");
		}
	}

	private void writeLockSet(final PrintWriter writer, final SummaryInfo info) {
		PackageFrag root = new PackageFrag(f_runDescription.getName());
		for (Field field : info.getEmptyLockSetFields()) {
			PackageFrag pkg = root;
			for (String frag : field.getPackage().split("\\.")) {
				pkg = pkg.frag(frag);
			}
			pkg = pkg.frag(field.getClazz());
			pkg.field(field);
		}
		writer.print("var packages = ");
		writer.print(root.toString());
		writer.print(";");

		root = new PackageFrag("");
		for (Field field : info.getEmptyLockSetFields()) {
			PackageFrag pkg = root;
			for (String frag : field.getPackage().split("\\.")) {
				pkg = pkg.frag(frag);
			}
			pkg = pkg.frag(field.getClazz());
			pkg.field(field);
		}
		writer.println();

		writer.print(String
				.format("var packages2 = { \"id\": \"%1$s\", \"name\": \"%1$s\", \"data\": {}, \"children\": [",
						f_runDescription.getName()));
		writer.print(root.buildNode(f_runDescription.getName(), ""));
		writer.println("]};");
	}

	private void writeCycles(final PrintWriter writer, final List<Cycle> cycles) {
		writer.println("var deadlocks = {");
		for (Iterator<Cycle> ci = cycles.iterator(); ci.hasNext();) {
			Cycle c = ci.next();
			TreeSet<EdgeEntry> edges = new TreeSet<EdgeEntry>();
			TreeSet<EdgeEntry> completed = new TreeSet<EdgeEntry>();
			for (Edge e : c.getEdges()) {
				edges.add(new EdgeEntry(e.getHeldId(), e.getAcquiredId()));
			}
			writer.printf("\tcycle%d : [", c.getNum());
			String heldId = null;
			boolean first = true;
			for (Iterator<Edge> ei = c.getEdges().iterator(); ei.hasNext();) {
				Edge e = ei.next();
				EdgeEntry entry = new EdgeEntry(e.getAcquiredId(),
						e.getHeldId());
				String arrowType;
				if (!e.getHeldId().equals(heldId)) {
					if (heldId != null) {
						writer.println("]\n}, ");
					}
					heldId = e.getHeldId();
					writer.println("{");
					writer.printf("\t\t\"id\": \"%s\",\n", e.getHeld());
					writer.printf("\t\t\"name\": \"%s\",\n", e.getHeld());
					writer.print("\t\t\"adjacencies\": [");
					first = true;
				}
				if (edges.contains(entry)) {
					arrowType = "doubleArrow";
					if (completed.contains(entry)) {
						// We have already marked this edge on a previous node
						continue;
					}
				} else {
					arrowType = "arrow";
				}
				if (!first) {
					writer.print(", ");
				}
				first = false;
				writer.printf(
						"{\"nodeTo\": \"%3$s\", \"data\": { \"$type\": \"%1$s\", \"$direction\": [\"%2$s\",\"%3$s\"]}}\n",
						arrowType, e.getHeld(), e.getAcquired());
				completed.add(new EdgeEntry(e.getHeldId(), e.getAcquiredId()));
			}
			writer.println("]\n}]");
			if (ci.hasNext()) {
				writer.print(", ");
			}
		}
		writer.println("};");
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
		head.javaScript("graph-data.js");
		head.javaScript("cycles.js");
	}

	private void loadTimeline(final Head head) {
		final URL tl = Thread
				.currentThread()
				.getContextClassLoader()
				.getResource(
						"/com/surelogic/common/js/timeline_libraries_v2.3.0.zip");
		File tlZip = new File(htmlDirectory, "timeline_libraries_v2.3.0.zip");
		FileUtility.copy(tl, tlZip);
		try {
			FileUtility.unzipFile(tlZip, htmlDirectory);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		tlZip.delete();
		head.javaScript("timeline_2.3.0/timeline_js/timeline-api.js?bundle=true");
		// head.javaScript("http://static.simile.mit.edu/timeline/api-2.3.0/timeline-api.js?bundle=true");
	}

}
