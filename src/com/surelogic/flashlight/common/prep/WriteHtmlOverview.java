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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.surelogic.common.FileUtility;
import com.surelogic.common.ImageWriter;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.flashlight.common.files.HtmlHandles;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Body;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Container;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Div;
import com.surelogic.flashlight.common.prep.HTMLBuilder.HTMLList;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Head;
import com.surelogic.flashlight.common.prep.HTMLBuilder.LI;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Row;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Table;
import com.surelogic.flashlight.common.prep.HTMLBuilder.UL;
import com.surelogic.flashlight.common.prep.SummaryInfo.Cycle;
import com.surelogic.flashlight.common.prep.SummaryInfo.Edge;
import com.surelogic.flashlight.common.prep.SummaryInfo.Field;
import com.surelogic.flashlight.common.prep.SummaryInfo.Site;

public final class WriteHtmlOverview implements IPostPrep {

	private static final int TABLE_LIMIT = 10;

	private static final String DATE_FORMAT = "yyyy.MM.dd-'at'-HH.mm.ss.SSS";
	private static final String GRAPH_DATE_FORMAT = "MMM dd yyyy HH:mm:ss zz";
	private static final String EMPTY_LOCK_SET_INSTANCES_QUERY = "5224d411-6ed3-432f-8b91-a1c43df22911";
	private static final String LOCK_CONTENTION_QUERY = "cb38a427-c259-4690-abe2-acea9661f737";
	private static final String LOCK_EDGE_QUERY = "c9453327-b892-4324-a6b8-2dceb32e1901";
	private static final String STATIC_LOCK_FREQUENCY_QUERY = "6a39e6ca-29e9-4093-ba03-0e9bd9503a1a";
	private static final String THREAD_BLOCKING_QUERY = "4e026769-1b0b-42bd-8893-f3b92add093f";
	private static final String THREAD_LOCKS_QUERY = "dc4b409d-6c6c-4d5f-87f6-c8223a9d40aa";
	private static final String LINE_OF_CODE_QUERY = "bd88cc1d-b606-463d-af27-b71c66ede24e";
	private static final String PACKAGE_IMG = "package.gif";
	private static final String CLASS_IMG = "class.gif";

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
			sidebar.a("#locks").h(2).text("Locks");
			sidebar.a("#fields").h(2).text("Fields");
			sidebar.a("#threads").h(2).text("Threads");
			Container content = main.div().id("content");
			main.div().clazz("clear");
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

			Container lockDiv = content.div().id("locks").clazz("tab");

			lockDiv.h(2).text("Locks");
			Div lockContentionDiv = lockDiv.div();
			lockContentionDiv.h(3).text("Lock Contention");
			List<SummaryInfo.Lock> locks = info.getLocks();
			if (locks.isEmpty()) {
				lockContentionDiv
						.p()
						.clazz("info")
						.text("No locks were detected in this run of the program.");
			} else {
				Table lockTable = lockContentionDiv.table();
				lockTable.header().th("Lock").th("Times Acquired")
						.th("Total Block Time").th("Average Block Time");

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
					link(lockTable.row().td().colspan(4),
							String.format("%d more results.", locks.size()
									- TABLE_LIMIT), LOCK_CONTENTION_QUERY);
				}
			}
			lockDiv.h(3).text("Potential Deadlocks");
			List<Cycle> cycles = info.getCycles();
			if (cycles.isEmpty()) {
				lockDiv.p()
						.clazz("info")
						.text("No lock cycles were detected in this run of the program.");
			} else {
				HTMLList deadlockList = lockDiv.ul().id("deadlock-list");
				for (Cycle cycle : cycles) {
					deadlockList.li().id("cycle" + cycle.getNum())
							.text(String.format("Cycle %d", cycle.getNum()));
				}
				writeCycles(graphs, cycles);
				Table dTable = lockDiv.table().id("deadlock-container");
				Row dRow = dTable.row();
				dRow.td().id("deadlock-widget");
				dRow.td().id("deadlock-threads");
			}
			Container fieldDiv = content.div().id("fields").clazz("tab");
			fieldDiv.h(2).text("Fields");
			Div lockSetDiv = fieldDiv.div();
			lockSetDiv.h(3).text("Shared Fields With No Lock Set");
			writeLockSet(graphs, info);
			displayLockSet(info, lockSetDiv);
			Container threadDiv = content.div().id("threads").clazz("tab");
			threadDiv.h(2).text("Threads");

			int threadCount = info.getThreads().size();
			threadDiv
					.p(String
							.format("%d threads were observed in this run of the program.",
									threadCount)).clazz("info");
			if (threadCount > 0) {
				threadDiv.div().id("tl");
			}
			writeThreadTimeline(graphs, info);

			threadDiv.h(3).text("Coverage");
			displayThreadCoverage(threadDiv, info.getThreadCoverage());

			threadDiv.h(3).text("Thread Liveness");

			Table threadTable = threadDiv.table();
			threadTable.header().th("Thread").th("Time Blocked");
			int count = 0;
			for (SummaryInfo.Thread thread : info.getThreads()) {
				final Row row = threadTable.row();
				link(row.td(), thread.getName(), THREAD_LOCKS_QUERY, "Thread",
						thread.getName(), "ThreadId", thread.getId());
				row.td(thread.getBlockTime() + " ns");
				if (++count == TABLE_LIMIT) {
					break;
				}
			}
			if (threadCount > TABLE_LIMIT) {
				link(threadTable.row().td().colspan(2),
						String.format("%d more results.", threadCount
								- TABLE_LIMIT), THREAD_BLOCKING_QUERY);
			}

			graphs.close();

			final HtmlHandles html = f_runDescription.getRunDirectory()
					.getHtmlHandles();
			html.writeIndexHtml(builder.build());
			writer.addImage(CLASS_IMG);
			writer.addImage(PACKAGE_IMG);
			writer.addImage("flashlight_overview_banner.png");
			writer.addImage("outline_down.png");
			writer.addImage("outline_filler.png");
			writer.addImage("outline_right.png");

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
				first = start;
			}
			if (last == null || last.getTime() < stop.getTime()) {
				last = stop;
			}
		}

		if (first == null) {
			writer.println("var timeline_data = 'none'");
		} else {
			writer.println("var timeline_data = {");
			writer.println(String.format(
					"'first': Timeline.DateTime.parseGregorianDateTime('%s'),",
					df.format(first)));
			writer.println(String.format(
					"'last': Timeline.DateTime.parseGregorianDateTime('%s'),",
					df.format(last)));
			long duration = last.getTime() - first.getTime();
			boolean hasOverviewBand = true;
			int mainIntervalPixels = 100;
			int overviewPixels = (int) Math.round(600 / (duration * .001 / 60));
			String mainInterval = "SECOND";
			String overviewInterval = "MINUTE";
			if (duration < 1000) {
				// Millisecond resolution
				mainInterval = "MILLISECOND";
				overviewInterval = "SECOND";
				overviewPixels = 600;
			} else if (duration < 10000) {
				// Second resolution
				hasOverviewBand = false;
				mainIntervalPixels = (int) (600 * 1000 / duration);
			} else if (duration > 1000 * 60 * 30) {
				mainInterval = "MINUTE";
				overviewInterval = "HOUR";
				overviewPixels = 100;
			}
			writer.printf(
					"'needsOverview': %s, 'mainBandInterval': Timeline.DateTime.%s, 'mainBandIntervalPixels': %d, 'overviewBandInterval': Timeline.DateTime.%s, 'overviewBandIntervalPixels': %d,\n",
					Boolean.toString(hasOverviewBand), mainInterval,
					mainIntervalPixels, overviewInterval, overviewPixels);
			writer.println("'dateTimeFormat': 'javascriptnative', ");
			writer.println("'events': [");
			for (SummaryInfo.Thread t : info.getThreads()) {
				Date start = t.getStart();
				Date stop = t.getStop();
				writer.print(String
						.format("{'start': %s, 'end': %s, 'title': \"%s\", 'description': 'Thread Duration: %(,.3f seconds\\nTime Blocked: %(,.3f seconds', 'durationEvent': true, 'color': 'blue' }",
								jsDate(start),
								jsDate(stop),
								t.getName(),
								(float) (stop.getTime() - start.getTime()) / 1000,
								(float) t.getBlockTime() / 1000000000));
				writer.println(",");
			}
			writer.print(String
					.format("{'start': %s, 'end': %s, 'title': \"%s\", 'description': 'Program Duration: %(,.3f seconds', 'durationEvent': true, 'color': 'red' }",
							jsDate(first), jsDate(last), "Program Run Time",
							(float) (last.getTime() - first.getTime()) / 1000));
			writer.println("]};");
		}
	}

	private void displayThreadCoverage(final Container threadDiv,
			final Site threadCoverage) {
		// The first node should be an anonymous root node, so we just do it's
		// children
		Set<Site> children = threadCoverage.getChildren();
		if (children.isEmpty()) {
			threadDiv.span().clazz("info")
					.text("There is no coverage data for this run.");
		} else {
			UL list = threadDiv.ul();
			list.clazz("outline").clazz("collapsed");
			for (Site child : children) {
				displayThreadCoverageHelper(list, child);
			}
		}
	}

	private void displayThreadCoverageHelper(final UL list, final Site coverage) {
		LI li = list.li();
		li.text(coverage.getPackage() + ".");
		Container emph = li.span().clazz("emph");
		emph.text(coverage.getClazz() + ".");
		String line = Integer.toString(coverage.getLine());
		link(emph, coverage.getLocation(), LINE_OF_CODE_QUERY, "Package",
				coverage.getPackage(), "Class", coverage.getClazz(), "Method",
				coverage.getLocation(), "Line", line);
		Set<Site> children = coverage.getChildren();
		if (children.isEmpty()) {
			return;
		}
		UL ul = li.ul();
		for (Site child : children) {
			displayThreadCoverageHelper(ul, child);
		}
	}

	private String jsList(final Collection<? extends Object> list) {
		StringBuilder b = new StringBuilder();
		b.append('[');
		for (Iterator<? extends Object> iter = list.iterator(); iter.hasNext();) {
			b.append('\"');
			b.append(iter.next().toString());
			b.append('\"');
			if (iter.hasNext()) {
				b.append(',');
			}
		}
		b.append(']');
		return b.toString();
	}

	private String jsDate(final Date date) {
		if (date == null) {
			return "?";
		}
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
		final String fromName;
		final String to;
		final String toName;
		final Set<String> threads;
		boolean bidirectional;

		EdgeEntry(final String from, final String fromName, final String to,
				final String toName, final Collection<String> threads) {
			this.from = from;
			this.fromName = fromName;
			this.to = to;
			this.toName = toName;
			this.threads = new HashSet<String>(threads);
		}

		@SuppressWarnings("unchecked")
		public EdgeEntry(final String from, final String to) {
			this(from, null, to, null, Collections.EMPTY_LIST);
		}

		public String getFrom() {
			return from;
		}

		public String getFromName() {
			return fromName;
		}

		public String getToName() {
			return toName;
		}

		public Set<String> getThreads() {
			return threads;
		}

		public void makeBidirectional(final List<String> threads2) {
			threads.addAll(threads2);
			bidirectional = true;
		}

		public boolean isBidirectional() {
			return bidirectional;
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
							.format("\"data\": { \"$area\": %d, \"$color\": \"#FF0000\", \"$lineWidth\": 1 }",
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
			Map<EdgeEntry, EdgeEntry> edges = new TreeMap<EdgeEntry, EdgeEntry>();
			for (Edge e : c.getEdges()) {
				EdgeEntry reverse = new EdgeEntry(e.getAcquiredId(),
						e.getHeldId());
				if (edges.containsKey(reverse)) {
					edges.get(reverse).makeBidirectional(e.getThreads());
				} else {
					EdgeEntry edge = new EdgeEntry(e.getHeldId(), e.getHeld(),
							e.getAcquiredId(), e.getAcquired(), e.getThreads());
					edges.put(edge, edge);
				}
			}
			writer.printf("\tcycle%d : [", c.getNum());
			String heldId = null;
			boolean first = true;
			for (Iterator<EdgeEntry> ei = edges.keySet().iterator(); ei
					.hasNext();) {
				EdgeEntry e = ei.next();
				if (!e.getFrom().equals(heldId)) {
					if (heldId != null) {
						writer.println("]\n}, ");
					}
					heldId = e.getFrom();
					writer.println("{");
					writer.printf("\t\t\"id\": \"%s\",\n", e.getFromName());
					writer.printf("\t\t\"name\": \"%s\",\n", e.getFromName());
					writer.print("\t\t\"adjacencies\": [");
					first = true;
				}
				final String arrowType = e.isBidirectional() ? "doubleArrow"
						: "arrow";
				if (!first) {
					writer.print(", ");
				}
				first = false;
				writer.printf(
						"{\"nodeTo\": \"%3$s\", \"data\": { \"$type\": \"%1$s\", \"$direction\": [\"%2$s\",\"%3$s\"], \"threads\": %4$s}}\n",
						arrowType, e.getFromName(), e.getToName(),
						jsList(e.getThreads()));
			}
			writer.println("]\n}]");
			if (ci.hasNext()) {
				writer.print(", ");
			}
		}
		writer.println("};");
		for (Cycle c : cycles) {
			writer.println("deadlocks.cycle" + c.getNum() + ".threads = "
					+ jsList(c.getThreads()) + ";");
		}
	}

	private void displayLockSet(final SummaryInfo info, final Container c) {
		UL packageList = c.ul();
		packageList.clazz("outline");
		Iterator<Field> fields = info.getEmptyLockSetFields().iterator();
		if (fields.hasNext()) {
			Field field = fields.next();
			String pakkage = field.getPackage();
			String clazz = field.getClazz();
			LI packageLI = packageList.li();
			packageLI.img(writer.imageLocation(PACKAGE_IMG)).alt("package");
			packageLI.span().clazz("emph").text(pakkage);
			UL classList = packageLI.ul();
			LI classLI = classList.li();
			classLI.img(writer.imageLocation(CLASS_IMG)).alt("class");
			classLI.span().clazz("emph").text(clazz);
			UL fieldList = classLI.ul();
			LI fieldLI = fieldList.li();
			fieldLink(fieldLI, field);
			while (fields.hasNext()) {
				field = fields.next();
				if (!pakkage.equals(field.getPackage())) {
					pakkage = field.getPackage();
					clazz = field.getClazz();
					packageLI = packageList.li();
					packageLI.img(writer.imageLocation(PACKAGE_IMG)).alt(
							"package");
					packageLI.span().clazz("emph").text(pakkage);
					classList = packageLI.ul();
					classLI = classList.li();
					classLI.img(writer.imageLocation(CLASS_IMG)).alt("class");
					classLI.span().clazz("emph").text(clazz);
					fieldList = classLI.ul();
				} else if (!clazz.equals(field.getClazz())) {
					clazz = field.getClazz();
					classLI = classList.li();
					classLI.img(writer.imageLocation(CLASS_IMG)).alt("class");
					classLI.span().clazz("emph").text(clazz);
					fieldList = classLI.ul();
				}
				fieldLI = fieldList.li();
				fieldLink(fieldLI, field);
			}
		} else {
			c.p()
					.clazz("info")
					.text("There are no fields accessed concurrently with an empty lock set in this run.");
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
	}

}
