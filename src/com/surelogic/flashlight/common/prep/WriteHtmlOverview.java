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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.surelogic.common.FileUtility;
import com.surelogic.common.ImageWriter;
import com.surelogic.common.derby.sqlfunctions.Trace;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.flashlight.common.files.HtmlHandles;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Body;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Cell;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Container;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Div;
import com.surelogic.flashlight.common.prep.HTMLBuilder.HTMLList;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Head;
import com.surelogic.flashlight.common.prep.HTMLBuilder.LI;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Row;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Span;
import com.surelogic.flashlight.common.prep.HTMLBuilder.TD;
import com.surelogic.flashlight.common.prep.HTMLBuilder.Table;
import com.surelogic.flashlight.common.prep.HTMLBuilder.UL;
import com.surelogic.flashlight.common.prep.SummaryInfo.BadPublishAccess;
import com.surelogic.flashlight.common.prep.SummaryInfo.BadPublishEvidence;
import com.surelogic.flashlight.common.prep.SummaryInfo.ContentionSite;
import com.surelogic.flashlight.common.prep.SummaryInfo.CoverageSite;
import com.surelogic.flashlight.common.prep.SummaryInfo.DeadlockEvidence;
import com.surelogic.flashlight.common.prep.SummaryInfo.DeadlockTrace;
import com.surelogic.flashlight.common.prep.SummaryInfo.Edge;
import com.surelogic.flashlight.common.prep.SummaryInfo.Loc;
import com.surelogic.flashlight.common.prep.SummaryInfo.Lock;
import com.surelogic.flashlight.common.prep.SummaryInfo.LockSetEvidence;
import com.surelogic.flashlight.common.prep.SummaryInfo.LockSetLock;
import com.surelogic.flashlight.common.prep.SummaryInfo.LockSetSite;
import com.surelogic.flashlight.common.prep.SummaryInfo.Site;
import com.surelogic.flashlight.common.prep.SummaryInfo.Thread;
import com.surelogic.flashlight.common.prep.json.JArray;
import com.surelogic.flashlight.common.prep.json.JObject;
import com.surelogic.flashlight.common.prep.json.JsonBuilder;

public final class WriteHtmlOverview implements IPostPrep {

    private static final String DATE_FORMAT = "yyyy.MM.dd-'at'-HH.mm.ss.SSS";
    private static final String GRAPH_DATE_FORMAT = "MMM dd yyyy HH:mm:ss zz";
    private static final String LOCK_CONTENTION_QUERY = "cb38a427-c259-4690-abe2-acea9661f737";
    private static final String LOCK_EDGE_QUERY = "c9453327-b892-4324-a6b8-2dceb32e1901";
    private static final String BAD_PUBLISH_QUERY = "708d1b8a-5274-4a25-b0b4-67f1a7a86690";
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

    @Override
    public void doPostPrep(final Connection c, final SLProgressMonitor mon)
            throws SQLException {
        mon.begin();
        try {
            SummaryInfo info = new SummaryInfo.SummaryQuery()
                    .perform(new ConnectionQuery(c));

            List<Category> categories = new ArrayList<Category>();

            Category locks = new Category("locks", "Locks");
            locks.getSections().add(new LockSection(info.getLocks()));
            locks.getSections().add(new DeadlocksSection(info.getDeadlocks()));

            Category fields = new Category("fields", "Fields");
            fields.getSections().add(
                    new LockSetSection(info.getEmptyLockSetFields()));
            fields.getSections().add(
                    new BadPublishSection(info.getBadPublishes()));

            Category threads = new Category("threads", "Threads");
            threads.getSections().add(new TimelineSection(info.getThreads()));
            threads.getSections().add(new CoverageSection(info));
            threads.getSections().add(new LivenessSection(info.getThreads()));
            categories.add(locks);
            categories.add(fields);
            categories.add(threads);
            final HtmlHandles html = f_runDescription.getRunDirectory()
                    .getHtmlHandles();
            displayPages(html, categories);
            loadJavaScript();
            loadTimeline();
            loadStyleSheet();
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

    private static class HeaderName {
        String name;
        String link;

        public HeaderName(final String name, final String link) {
            this.name = name;
            this.link = link;
        }

    }

    private HTMLBuilder displayPage(final List<HeaderName> headers,
            final Section s) {
        HTMLBuilder builder = new HTMLBuilder();
        Head head = builder.head(f_runDescription.getName());
        head.styleSheet("RunOverviewStyleSheet.css");
        head.javaScript("jquery-1.5.1.min.js");
        head.javaScript("overview.js");
        if (s != null) {
            for (String js : s.getJavaScriptImports()) {
                head.javaScript(js);
            }
        }

        Body body = builder.body();
        body.div().id("header").h(1).text(f_runDescription.getName());
        Container main = body.div().id("main");
        Container content = main.div().id("content");
        main.div().clazz("clear");
        HTMLList sectionList = content.ul().clazz("sectionList");
        for (HeaderName hn : headers) {
            LI li = sectionList.li();
            if (s != null && s.getName().equals(hn.name)) {
                li.clazz("selected");
            }
            li.a(hn.link).text(hn.name);
        }
        content.div().clazz("clear");
        if (s != null) {
            s.display(content);
        }
        return builder;
    }

    private void displayPages(final HtmlHandles html,
            final List<Category> categories) {
        List<HeaderName> headers = new ArrayList<HeaderName>();
        int index = 1;
        for (Category c : categories) {
            for (Section s : c.getSections()) {
                String name = s.getName();
                String link = "index" + index++ + ".html";
                HeaderName hn = new HeaderName(name, link);
                headers.add(hn);
            }
        }
        index = 0;
        html.writeIndexHtml(displayPage(headers, null).build());
        for (Category c : categories) {
            for (Section s : c.getSections()) {
                html.writeHtml(headers.get(index++).link,
                        displayPage(headers, s).build());
            }
        }
    }

    private class LockSection extends Section {

        private final List<Lock> locks;

        public LockSection(final List<SummaryInfo.Lock> locks) {
            super("Lock Contention");
            this.locks = locks;
        }

        @Override
        void displaySection(final Container c) {
            if (locks.isEmpty()) {
                c.p()
                        .clazz("info")
                        .text("No locks were detected in this run of the program.");
            } else {
                Table lockTable = c.table().id("lockTable");
                displayTreeTable(locks, lockTable, new LockTableRowProvider());

                if (locks.size() >= SummaryInfo.LOCK_LIMIT) {
                    buildQueryLink(
                            lockTable.row().td().colspan(4).clazz("leaf")
                                    .clazz("depth1"),
                            String.format("More results...", locks.size()
                                    - SummaryInfo.LOCK_LIMIT),
                            LOCK_CONTENTION_QUERY);
                }
            }

        }

    }

    private static class LockTableRowProvider implements RowProvider<Lock> {

        @Override
        public void headerRow(final Row row) {
            row.th("Lock").th("Times Acquired").th("Average Block Time")
                    .th("Total Block Time");
        }

        @Override
        public void row(final Table table, final Lock lock) {
            Row lockRow = table.row();
            lockRow.td().clazz("depth1").text(lock.getName());
            lockRow.td(lock.getAcquired());
            lockRow.td(lock.getAverageBlock() + " ns");
            lockRow.td(lock.getBlockTime() + " ns");
            String curPakkage = null;
            long pakkageDuration = 0, clazzDuration = 0;
            Row pakkageRow = null;
            Row clazzRow = null;
            String curClazz = null;
            for (ContentionSite s : lock.getContentionSites()) {
                Site site = s.getSite();
                String pakkage = site.getPackage();
                String clazz = site.getClazz();
                if (!pakkage.equals(curPakkage)) {
                    if (pakkageRow != null) {
                        pakkageRow.td(pakkageDuration + " ns");
                    }
                    pakkageDuration = clazzDuration = 0;
                    curPakkage = pakkage;
                    curClazz = null;
                    pakkageRow = table.row();
                    pakkageRow.td().clazz("depth2").span().clazz("package")
                            .text(pakkage);
                    pakkageRow.td();
                    pakkageRow.td();
                }
                if (!clazz.equals(curClazz)) {
                    if (clazzRow != null) {
                        clazzRow.td(clazzDuration + " ns");
                    }
                    clazzDuration = 0;
                    curClazz = clazz;
                    clazzRow = table.row();
                    clazzRow.td().clazz("depth3").span().clazz("class")
                            .text(clazz);
                    clazzRow.td();
                    clazzRow.td();
                }
                Row r = table.row();
                Container locTd = r.td().clazz("depth4").clazz("leaf");
                for (int i = 0; i < 2; i++) {
                    r.td();
                }
                Span span = locTd.span();
                span.text(" at " + site.getLocation());
                buildCodeLink(span, "(" + site.getFile() + ":" + site.getLine()
                        + ")", "Package", site.getPackage(), "Class",
                        site.getClazz(), "Method", site.getLocation(), "Line",
                        Integer.toString(site.getLine()));
                r.td().text(s.getDurationNs() + " ns");
                pakkageDuration += s.getDurationNs();
                clazzDuration += s.getDurationNs();
            }
            pakkageRow.td(pakkageDuration + " ns");
            clazzRow.td(clazzDuration + " ns");
        }

        @Override
        public int numCols(final Lock t) {
            return 4;
        }

        @Override
        public List<Cell> cellTypes() {
            return Arrays.asList(new Cell[] { Cell.TEXT, Cell.NUMBER,
                    Cell.NUMBER, Cell.NUMBER });
        }

    }

    private class DeadlocksSection extends Section {

        private final List<DeadlockEvidence> deadlocks;

        public DeadlocksSection(final List<DeadlockEvidence> deadlocks) {
            super("Deadlocks");
            this.deadlocks = deadlocks;
        }

        @Override
        public List<String> getJavaScriptImports() {
            return Arrays.asList(new String[] { "excanvas.js", "jit.js",
                    "graph-data.js" });
        }

        @Override
        void displaySection(final Container c) {
            if (deadlocks.isEmpty()) {
                c.p()
                        .clazz("info")
                        .text("No lock cycles were detected in this run of the program.");
            } else {
                HTMLList deadlockList = c.ul().id("deadlock-list");
                for (DeadlockEvidence deadlock : deadlocks) {
                    deadlockList.li().id("cycle" + deadlock.getNum())
                            .text(String.format("Cycle %d", deadlock.getNum()));
                }
                PrintWriter graphs = null;
                try {
                    graphs = new PrintWriter(new File(htmlDirectory,
                            "graph-data.js"));
                    writeCycles(graphs, deadlocks);
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException(e);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                } finally {
                    if (graphs != null) {
                        graphs.close();
                    }
                }

                Table dTable = c.table().id("deadlock-container");
                Row dRow = dTable.row();
                dRow.td().id("deadlock-widget");
                TD col = dRow.td();
                col.h(2).text("Edges");
                Container edges = col.div().id("deadlock-edges");
                col.h(2).text("Threads");
                col.div().id("deadlock-threads");
                Container traces = dTable.row().td().colspan(2)
                        .id("deadlock-traces");
                for (DeadlockEvidence de : deadlocks) {
                    Div traceDiv = traces.div();
                    traceDiv.id("deadlock-traces-cycle" + de.getNum()).clazz(
                            "deadlock-cycle-traces");
                    Div edgeDiv = edges.div();
                    edgeDiv.id("deadlock-edges-cycle" + de.getNum()).clazz(
                            "deadlock-cycle-edges");
                    edgeDiv.ul().clazz("deadlock-trace-menu");
                    List<DeadlockTrace> traceList = new ArrayList<DeadlockTrace>(
                            de.getTraces().values());
                    Collections.sort(traceList);
                    for (DeadlockTrace t : traceList) {
                        Edge edge = t.getEdge();
                        String edgeId = "deadlock-trace-" + edgeId(edge);
                        // TODO List<LockTrace> lockTrace = t.getLockTrace();
                        Div div = traceDiv.div();
                        div.clazz("deadlock-trace-edge").id(edgeId);
                        div.h(4).text("Lock Acquired: ");
                        div.span().text(edge.getAcquired());
                        UL ul = div.ul();
                        displayTraceList(ul, t.getTrace());
                        div.h(4).text("Lock Held: ");
                        div.span().text(edge.getHeld());
                        ul = div.ul();
                        displayTraceList(ul, t.getHeldTrace());
                    }
                }
            }

        }

        private void writeCycles(final PrintWriter writer,
                final List<DeadlockEvidence> deadlocks) throws IOException {
            JsonBuilder builder = new JsonBuilder();
            JObject jDeadlocks = builder.object("deadlocks");
            for (Iterator<DeadlockEvidence> des = deadlocks.iterator(); des
                    .hasNext();) {
                DeadlockEvidence de = des.next();
                Map<EdgeEntry, EdgeEntry> edges = new TreeMap<EdgeEntry, EdgeEntry>();
                // Calculate edge entries from the deadlock graph
                for (Edge e : de.getEdges()) {
                    EdgeEntry reverse = new EdgeEntry(e.getAcquiredId(),
                            e.getHeldId());
                    // Check to see if we have seen this edge's reverse. If so,
                    // make the existing edge entry bidirectional. Otherwise
                    // just add the edge entry as normal.
                    if (edges.containsKey(reverse)) {
                        edges.get(reverse).makeBidirectional(e.getThreads());
                    } else {
                        EdgeEntry edge = new EdgeEntry(edgeId(e),
                                e.getHeldId(), e.getHeld(), e.getAcquiredId(),
                                e.getAcquired(), e.getThreads());
                        edges.put(edge, edge);
                    }
                }
                // Write out edge entries into the list of cycles
                JArray jCycle = jDeadlocks.array("cycle" + de.getNum());
                String heldId = null;
                JArray jAdj = null;
                for (EdgeEntry e : edges.keySet()) {
                    if (!e.getFrom().equals(heldId)) {
                        heldId = e.getFrom();
                        JObject jEdge = jCycle.object();
                        jEdge.val("id", e.getFromName());
                        jEdge.val("name", e.getFromName());
                        jAdj = jEdge.array("adjacencies");
                    }
                    JObject jAdjObj = jAdj.object();
                    jAdjObj.val("nodeTo", e.getToName());
                    JObject jAdjData = jAdjObj.object("data");
                    jAdjData.val("$type", e.isBidirectional() ? "doubleArrow"
                            : "arrow");
                    jAdjData.array("$direction", e.getFromName(), e.getToName());
                    jAdjData.val("threads", e.getThreads());
                    jAdjData.val("edgeIds", e.getIds());
                }
            }
            for (DeadlockEvidence de : deadlocks) {
                builder.val("deadlocks.cycle" + de.getNum() + ".threads",
                        de.getThreads());
                List<DeadlockTrace> traceList = new ArrayList<SummaryInfo.DeadlockTrace>(
                        de.getTraces().values());
                Collections.sort(traceList);
                JArray jEdges = builder.array("deadlocks.cycle" + de.getNum()
                        + ".edges");
                for (DeadlockTrace t : traceList) {
                    Edge edge = t.getEdge();
                    String edgeName = edge.getHeld() + " -> "
                            + edge.getAcquired();
                    jEdges.object("id", edgeId(edge), "name", edgeName);
                }
            }
            builder.build(writer);
        }
    }

    private static final String EDGE_ID_SEP = "--";

    private static String edgeId(final Edge edge) {
        return edge.getHeld().replace('$', '.') + EDGE_ID_SEP
                + edge.getAcquired().replace('$', '.');
    }

    private class LockSetSection extends Section {
        private final List<LockSetEvidence> fields;

        public LockSetSection(final List<LockSetEvidence> fields) {
            super("Race Conditions");
            this.fields = fields;
        }

        @Override
        void displaySection(final Container c) {
            if (!fields.isEmpty()) {
                displayClassTree(fields, c, new LockSetLink());
                c.hr();
                for (LockSetEvidence field : fields) {
                    if (field.getLikelyLocks().isEmpty()) {
                        c.div()
                                .id("locksets-" + field.getPackage() + "."
                                        + field.getClazz() + "."
                                        + field.getName())
                                .clazz("info")
                                .clazz("locksetoutline")
                                .text("No locks were held when this field was accessed.");
                    } else {
                        displayTreeTable(
                                Collections.singletonList(field),
                                c.table()
                                        .clazz("locksetoutline")
                                        .id("locksets-" + field.getPackage()
                                                + "." + field.getClazz() + "."
                                                + field.getName()),
                                new LockSetRowProvider());
                    }
                }
            } else {
                c.p()
                        .clazz("info")
                        .text("There are no fields accessed concurrently with an empty lock set in this run.");
            }
        }
    }

    static class LockSetLink implements LinkProvider<LockSetEvidence> {
        @Override
        public void link(final Container c, final LockSetEvidence field) {
            c.a("#locksets-" + field.getPackage() + "." + field.getClazz()
                    + "." + field.getName()).clazz("locksetlink")
                    .clazz("field").text(field.getName());
        }
    }

    private static class LockSetRowProvider implements
            RowProvider<LockSetEvidence> {
        private final LinkProvider<LockSetSite> heldProvider = new LinkProvider<LockSetSite>() {
            @Override
            public void link(final Container c, final LockSetSite t) {
                Span span = c.span();
                span.text(" at " + t.getLocation());
                buildCodeLink(span,
                        "(" + t.getFile() + ":" + t.getLine() + ")", "Package",
                        t.getPackage(), "Class", t.getClazz(), "Method",
                        t.getLocation(), "Line", Integer.toString(t.getLine()));
                Site a = t.getAcquiredAt();
                span.text(" via " + a.getLocation());
                buildCodeLink(span,
                        "(" + a.getFile() + ":" + a.getLine() + ")", "Package",
                        a.getPackage(), "Class", a.getClazz(), "Method",
                        a.getLocation(), "Line", Integer.toString(a.getLine()));
            }
        };
        private final LinkProvider<Site> notHeldProvider = new LinkProvider<Site>() {
            @Override
            public void link(final Container c, final Site t) {
                Span span = c.span();
                span.text(" at " + t.getLocation());
                buildCodeLink(span,
                        "(" + t.getFile() + ":" + t.getLine() + ")", "Package",
                        t.getPackage(), "Class", t.getClazz(), "Method",
                        t.getLocation(), "Line", Integer.toString(t.getLine()));
            }
        };

        @Override
        public int numCols(final LockSetEvidence t) {
            return 3;
        }

        @Override
        public void row(final Table table, final LockSetEvidence e) {
            for (LockSetLock lock : e.getLikelyLocks()) {
                Row lockRow = table.row();
                lockRow.td().clazz("depth1").text(lock.getName());
                lockRow.td(lock.getTimesAcquired());
                lockRow.td(lock.getHeldPercentage());
                Row heldRow = table.row();
                heldRow.td().clazz("depth2").text("held at");
                heldRow.td();
                heldRow.td();
                Row heldTreeRow = table.row();
                displayClassTree(lock.getHeldAt(),
                        heldTreeRow.td().clazz("depth3").clazz("leaf"),
                        heldProvider);
                heldTreeRow.td();
                heldTreeRow.td();
                Row notHeldRow = table.row();
                notHeldRow.td().clazz("depth2").text("not held at");
                Row notHeldTreeRow = table.row();
                displayClassTree(lock.getNotHeldAt(), notHeldTreeRow.td()
                        .clazz("depth3").clazz("leaf"), notHeldProvider);
                notHeldTreeRow.td();
                notHeldTreeRow.td();
            }
        }

        @Override
        public void headerRow(final Row row) {
            row.th("Lock").th("# Acquisitions").th("% Held");
        }

        @Override
        public List<Cell> cellTypes() {
            return Arrays.asList(new Cell[] { Cell.TEXT, Cell.NUMBER,
                    Cell.NUMBER });
        }
    }

    class BadPublishSection extends Section {

        private final List<BadPublishEvidence> badPublishes;

        public BadPublishSection(final List<BadPublishEvidence> badPublishes) {
            super("Bad Publishes");
            this.badPublishes = badPublishes;
        }

        @Override
        void displaySection(final Container c) {
            if (!badPublishes.isEmpty()) {
                Table table = c.table();
                c.hr();
                Div div = c.div();
                displayClassTreeTable(badPublishes, table, new BadPublishTable(
                        div));
                // displayClassTree(badPublishes, c, new BadPublishLink());
            } else {
                c.p()
                        .clazz("info")
                        .text("Flashlight detected no improperly published fields.");
            }

        }

    }

    private static class BadPublishTable implements
            RowProvider<BadPublishEvidence> {

        private final Container traces;

        BadPublishTable(final Container traces) {
            this.traces = traces;
        }

        @Override
        public void headerRow(final Row row) {
            row.th("Package/Class/Field/Time").th("Thread").th("Read");
        }

        @Override
        public void row(final Table table, final BadPublishEvidence e) {
            Row fieldRow = table.row();
            fieldRow.td().clazz("depth3").clazz("field").text(e.getName());
            fieldRow.td();
            fieldRow.td();
            SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
            for (BadPublishAccess a : e.getAccesses()) {
                Row accessRow = table.row();
                String id = "badPublishTrace-"
                        + a.getTime().toString().replace(' ', '-') + "-"
                        + e.getPackage() + "." + e.getClazz() + "."
                        + e.getName();
                accessRow.td().clazz("leaf").clazz("depth4").clazz("thread")
                        .a('#' + id).clazz("badPublishTraceLink")
                        .text(df.format(a.getTime()));
                accessRow.td().text(a.getThread());
                accessRow.td().text(a.isRead() ? "R" : "W");
                displayTrace(id, e, a);
            }
        }

        @Override
        public int numCols(final BadPublishEvidence e) {
            return 3;
        }

        void displayTrace(final String id, final BadPublishEvidence e,
                final BadPublishAccess a) {
            List<Trace> trace = a.getTrace();
            UL ul = traces.ul();
            ul.clazz("badPublishTrace").id(id);
            LI accessLi = ul.li();
            accessLi.text(String.format("Access of %s.%s.%s", e.getPackage(),
                    e.getClazz(), e.getName()));
            LI traceLI = ul.li();
            ul = traceLI.ul();
            displayTraceList(ul, trace);
        }

        @Override
        public List<Cell> cellTypes() {
            return Arrays
                    .asList(new Cell[] { Cell.TEXT, Cell.TEXT, Cell.TEXT });
        }
    }

    static void displayTraceList(final UL ul, final List<Trace> trace) {
        for (Trace t : trace) {
            LI li = ul.li();
            li.text(" at ");
            displayLocation(li, t.getPackage(), t.getClazz(), t.getLoc(),
                    t.getFile(), t.getLine());
        }
    }

    static void displayLocation(final Container c, final String pakkage,
            final String clazz, final String loc, final String file,
            final int line) {
        Container emph = c.span();
        emph.text(pakkage + "." + clazz + "." + loc);
        String lin = Integer.toString(line);
        buildCodeLink(emph, "(" + file + ":" + lin + ")", "Package", pakkage,
                "Class", clazz, "Method", loc, "Line", lin);
    }

    static void displayLocation(final Container c, final String pakkage,
            final String clazz, final String loc, final String file) {
        Container emph = c.span();
        emph.text(pakkage + "." + clazz + "." + loc);
        buildCodeLink(emph, "(" + file + ")", "Package", pakkage, "Class",
                clazz, "Method", loc);
    }

    class TimelineSection extends Section {

        private final List<Thread> threads;

        public TimelineSection(final List<Thread> threads) {
            super("Program Timeline");
            this.threads = threads;
        }

        @Override
        public List<String> getJavaScriptImports() {
            return Arrays.asList(new String[] {
                    "timeline_2.3.0/timeline_js/timeline-api.js?bundle=true",
                    "timeline-data.js" });
        }

        @Override
        void displaySection(final Container c) {
            int threadCount = threads.size();
            c.p(String.format(
                    "%d threads were observed in this run of the program.",
                    threadCount)).clazz("info");
            if (threadCount > 0) {
                c.div().id("tl");
            }
            PrintWriter graphs = null;
            try {
                graphs = new PrintWriter(new File(htmlDirectory,
                        "timeline-data.js"));
                writeThreadTimeline(graphs);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                if (graphs != null) {
                    graphs.close();
                }
            }

        }

        private void writeThreadTimeline(final PrintWriter writer)
                throws IOException {
            SimpleDateFormat df = new SimpleDateFormat(GRAPH_DATE_FORMAT);
            Date first = null, last = null;
            for (Thread t : threads) {
                Date start = t.getStart();
                Date stop = t.getStop();
                if (first == null || start.getTime() < first.getTime()) {
                    first = start;
                }
                if (last == null || last.getTime() < stop.getTime()) {
                    last = stop;
                }
            }
            JsonBuilder builder = new JsonBuilder();
            if (first == null) {
                builder.string("timeline_data", "none");
            } else {
                JObject jTimeline = builder.object("timeline_data");
                jTimeline.literal("first", String.format(
                        "Timeline.DateTime.parseGregorianDateTime('%s')",
                        df.format(first)));
                jTimeline.literal("last", String.format(
                        "Timeline.DateTime.parseGregorianDateTime('%s')",
                        df.format(last)));
                long duration = last.getTime() - first.getTime();
                boolean hasOverviewBand = true;
                int mainIntervalPixels = 100;
                int overviewPixels = (int) Math
                        .round(600 / (duration * .001 / 60));
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
                jTimeline.bool("needsOverview", hasOverviewBand);
                jTimeline.literal("mainBandInterval", "Timeline.DateTime."
                        + mainInterval);
                jTimeline.val("mainBandIntervalPixels", mainIntervalPixels);
                jTimeline.literal("overviewBandInterval", "Timeline.DateTime."
                        + overviewInterval);
                jTimeline.val("overviewBandIntervalPixels", overviewPixels);
                jTimeline.string("dateTimeFormat", "javascriptnative");
                JArray jEvents = jTimeline.array("events");
                for (Thread t : threads) {
                    Date start = t.getStart();
                    Date stop = t.getStop();
                    JObject jDate = jEvents.object();
                    jDate.literal("start", jsDate(start)).literal("end",
                            jsDate(stop));
                    jDate.string("title", t.getName());
                    jDate.string(
                            "description",
                            String.format(
                                    "Thread Duration: %(,.3f seconds\nTime Blocked: %(,.3f seconds",
                                    (float) (stop.getTime() - start.getTime()) / 1000,
                                    (float) t.getBlockTime() / 1000000000));
                    jDate.bool("durationEvent", true).string("color", "blue");
                }
                JObject jTotal = jEvents.object();
                jTotal.literal("start", jsDate(first)).literal("end",
                        jsDate(last));
                jTotal.string("title", "Program Run Time");
                jTotal.string("description", String.format(
                        "Duration: %(,.3f seconds",
                        (float) (last.getTime() - first.getTime()) / 1000));
                jTotal.bool("durationEvent", true).string("color", "red");
            }
            builder.build(writer);
        }
    }

    class CoverageSection extends Section {

        private final CoverageSite site;
        private final List<Thread> threads;

        public CoverageSection(final SummaryInfo info) {
            super("Coverage");
            this.site = info.getThreadCoverage();
            threads = info.getThreads();
        }

        @Override
        public List<String> getJavaScriptImports() {
            return Collections.singletonList("coverage-data.js");
        }

        @Override
        void displaySection(final Container c) {
            // The first node should be an anonymous root node, so we just do
            // its children
            Set<CoverageSite> children = site.getChildren();
            if (children.isEmpty()) {
                c.p().clazz("info")
                        .text("There is no coverage data for this run.");
            } else {
                Table threadTable = c.table();
                Row threadRow = threadTable.row();
                TD threadDiv = threadRow.td();
                threadDiv.id("thread-td");
                threadDiv.h(3).text("Threads");
                threadDiv.ul().id("threads");
                TD coverageDiv = threadRow.td();
                coverageDiv.id("coverage-td");
                coverageDiv.h(3).text("Code Coverage");
                coverageDiv.div().id("coverage").ul();
            }

            PrintWriter graphs = null;
            try {
                graphs = new PrintWriter(new File(htmlDirectory,
                        "coverage-data.js"));
                writeThreadCoverage(graphs);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                if (graphs != null) {
                    graphs.close();
                }
            }
        }

        void writeThreadCoverage(final PrintWriter writer) throws IOException {
            JsonBuilder builder = new JsonBuilder();
            JObject jThreads = builder.object("threads");
            for (Thread t : threads) {
                JObject jThread = jThreads.object(t.getId());
                jThread.val("blockTime", t.getBlockTime());
                jThread.string("name", t.getName());
                jThread.string("start", t.getStart().toString());
                jThread.string("stop", t.getStop().toString());
            }
            JObject jRoot = builder.object("coverage");
            writeNodes(jRoot, site);
            builder.build(writer);
        }

        private void writeNodes(final JObject jRoot, final CoverageSite site) {
            for (CoverageSite cs : site.getChildren()) {
                String id = cs.getPackage() + "." + cs.getClazz() + "."
                        + cs.getLocation();
                JObject jSite = jRoot.object(id);
                jSite.object("site", "clazz", cs.getClazz(), "pakkage",
                        cs.getPackage(), "file", cs.getFile(), "line",
                        cs.getLine(), "location", cs.getLocation());
                jSite.val("threadsSeen", cs.getThreadsSeen());
                writeNodes(jSite.object("children"), cs);
            }
        }

    }

    class LivenessSection extends Section {
        private final List<Thread> threads;

        public LivenessSection(final List<Thread> threads) {
            super("Liveness");
            this.threads = new ArrayList<Thread>(threads);
            Collections.sort(this.threads, new Comparator<Thread>() {
                @Override
                public int compare(final Thread o1, final Thread o2) {
                    long t1 = o1.getBlockTime();
                    long t2 = o2.getBlockTime();
                    return t1 > t2 ? -1 : t1 == t2 ? 0 : 1;
                }
            });
        }

        @Override
        void displaySection(final Container c) {
            int TABLE_LIMIT = 100;
            Table threadTable = c.table();
            threadTable.header().th("Thread").th("Time Blocked");
            int count = 0;
            for (Thread thread : threads) {
                final Row row = threadTable.row();
                buildQueryLink(row.td(), thread.getName(), THREAD_LOCKS_QUERY,
                        "Thread", thread.getName(), "ThreadId", thread.getId());
                row.td(thread.getBlockTime() + " ns");
                if (++count == TABLE_LIMIT) {
                    break;
                }
            }
            if (threads.size() >= TABLE_LIMIT) {
                buildQueryLink(
                        threadTable.row().td().colspan(2),
                        String.format("%d more results.", threads.size()
                                - TABLE_LIMIT), THREAD_BLOCKING_QUERY);
            }

        }
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
     * the graph. It has an optional id which is not used for equality, but is
     * used when an edge is written out to identify it.
     * 
     * @author nathan
     * 
     */
    private static class EdgeEntry implements Comparable<EdgeEntry> {
        final String id;
        final String from;
        final String fromName;
        final String to;
        final String toName;
        final Set<String> threads;
        boolean bidirectional;

        EdgeEntry(final String id, final String from, final String fromName,
                final String to, final String toName,
                final Collection<String> threads) {
            this.id = id;
            this.from = from;
            this.fromName = fromName;
            this.to = to;
            this.toName = toName;
            this.threads = new HashSet<String>(threads);
        }

        public List<String> getIds() {
            if (bidirectional) {
                final List<String> ids = new ArrayList<String>(2);
                ids.add(id);
                // XXX We are making an assumption that the form of id returned
                // by edgeId splits the node names with an --
                int split = id.indexOf(EDGE_ID_SEP);
                String first = id.substring(0, split);
                String second = id.substring(split + 2);
                ids.add(second + EDGE_ID_SEP + first);
                return ids;
            } else {
                return Collections.singletonList(id);
            }
        }

        @SuppressWarnings("unchecked")
        public EdgeEntry(final String from, final String to) {
            this("", from, null, to, null, Collections.EMPTY_LIST);
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

        @Override
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

    private static <T extends Loc> void displayClassTreeTable(
            final List<T> fields, final Table t, final RowProvider<T> lp) {
        t.clazz("treeTable");
        lp.headerRow(t.header());
        String curPakkage = null, curClazz = null;
        for (T f : fields) {
            String pakkage = f.getPackage();
            String clazz = f.getClazz();
            if (!pakkage.equals(curPakkage)) {
                curPakkage = pakkage;
                curClazz = null;
                Row r = t.row();
                r.td().clazz("depth1").span().clazz("package").text(pakkage);
                for (int i = 1; i < lp.numCols(f); i++) {
                    r.td();
                }
            }
            if (!clazz.equals(curClazz)) {
                curClazz = clazz;
                Row r = t.row();
                r.td().clazz("depth2").span().clazz("class").text(clazz);
                for (int i = 1; i < lp.numCols(f); i++) {
                    r.td();
                }
            }
            lp.row(t, f);
        }
    }

    private static <T> void displayTreeTable(final List<T> ts,
            final Table table, final RowProvider<T> rp) {
        table.clazz("treeTable");
        table.cellTypes(rp.cellTypes());
        rp.headerRow(table.header());
        for (T f : ts) {
            rp.row(table, f);
        }
    }

    interface RowProvider<T> {
        void headerRow(final Row row);

        void row(final Table table, T t);

        int numCols(T t);

        List<Cell> cellTypes();
    }

    /**
     * Displays a tree of fields.
     * 
     * @param fields
     *            a list of fields in alphabetical order by package/class/field
     * @param c
     *            the container to write to
     */
    private static <T extends Loc> void displayClassTree(final List<T> fields,
            final Container c, final LinkProvider<T> lp) {
        Iterator<T> iter = fields.iterator();
        if (iter.hasNext()) {
            UL packageList = c.ul();
            packageList.clazz("outline");
            T field = iter.next();
            String pakkage = field.getPackage();
            String clazz = field.getClazz();
            LI packageLI = packageList.li();
            packageLI.span().clazz("package").text(pakkage);
            UL classList = packageLI.ul();
            LI classLI = classList.li();
            classLI.span().clazz("class").text(clazz);
            UL fieldList = classLI.ul();
            LI fieldLI = fieldList.li();
            lp.link(fieldLI, field);

            while (iter.hasNext()) {
                field = iter.next();
                if (!pakkage.equals(field.getPackage())) {
                    pakkage = field.getPackage();
                    clazz = field.getClazz();
                    packageLI = packageList.li();
                    packageLI.span().clazz("package").text(pakkage);
                    classList = packageLI.ul();
                    classLI = classList.li();
                    classLI.span().clazz("class").text(clazz);
                    fieldList = classLI.ul();
                } else if (!clazz.equals(field.getClazz())) {
                    clazz = field.getClazz();
                    classLI = classList.li();
                    classLI.span().clazz("class").text(clazz);
                    fieldList = classLI.ul();
                }
                fieldLI = fieldList.li();
                lp.link(fieldLI, field);
            }
        }
    }

    interface LinkProvider<T> {
        void link(final Container c, T t);
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
    private static void buildCodeLink(final Container c, final String text,
            final String... args) {
        StringBuilder b = new StringBuilder();
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "There must be an even number of arguments");
        }
        b.append("index.html?loc=");
        for (int i = 0; i < args.length; i = i + 2) {
            b.append('&');
            b.append(args[i]);
            b.append('=');
            b.append(args[i + 1]);
        }
        c.a(b.toString()).text(text);
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
    private static void buildQueryLink(final Container c, final String text,
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

    @Override
    public String getDescription() {
        return "Generating an HTML overview of the results of this run";
    }

    /**
     * Loads and includes the Javadoc hover style sheet.
     * 
     * @return the style sheet, or <code>null</code> if unable to load
     */
    private void loadStyleSheet() {
        final URL styleSheetURL = java.lang.Thread
                .currentThread()
                .getContextClassLoader()
                .getResource(
                        "/com/surelogic/flashlight/common/prep/RunOverviewStyleSheet.css");
        FileUtility.copy(styleSheetURL, new File(htmlDirectory,
                "RunOverviewStyleSheet.css"));
    }

    /**
     * Loads and returns the outline JavaScript script.
     * 
     * @param s
     * 
     * @return the style sheet, or <code>null</code> if unable to load
     */
    private void loadJavaScript() {
        final ClassLoader loader = java.lang.Thread.currentThread()
                .getContextClassLoader();
        final URL vis = loader.getResource("/com/surelogic/common/js/jit.js");
        final URL jquery = loader
                .getResource("/com/surelogic/common/js/jquery-1.5.1.min.js");
        final URL canvas = loader
                .getResource("/com/surelogic/common/js/excanvas.js");
        final URL cycles = loader
                .getResource("/com/surelogic/flashlight/common/prep/overview.js");
        FileUtility.copy(cycles, new File(htmlDirectory, "overview.js"));
        FileUtility.copy(canvas, new File(htmlDirectory, "excanvas.js"));
        FileUtility.copy(vis, new File(htmlDirectory, "jit.js"));
        FileUtility
                .copy(jquery, new File(htmlDirectory, "jquery-1.5.1.min.js"));

    }

    private void loadTimeline() {
        final URL tl = java.lang.Thread
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
    }

    private class Category {

        private final String id;
        private final String name;
        private final List<Section> sections;

        public Category(final String id, final String name) {
            this.name = name;
            this.id = id;
            sections = new ArrayList<Section>();
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<Section> getSections() {
            return sections;
        }

        void display(final Container c) {
            // c.h(2).text(name);
            // HTMLList sectionList = c.ul().clazz("sectionList");
            int i = 0;
            for (Section s : sections) {
                // sectionList.li().a('#' + id + i).text(s.getName());
                Container sDiv = c.div().clazz("section").id(id + i);
                s.display(sDiv);
                ++i;
            }
        }
    }

    private static abstract class Section {
        private final String name;

        public Section(final String name) {
            this.name = name;
        }

        public List<String> getJavaScriptImports() {
            return Collections.emptyList();
        }

        public String getName() {
            return name;
        }

        void display(final Container c) {
            displaySection(c);
        }

        abstract void displaySection(final Container c);

    }
}
