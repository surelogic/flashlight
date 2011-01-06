package com.surelogic.flashlight.common.prep;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.surelogic.common.xml.XMLUtil;

/**
 * A simple builder for HTML pages.
 * 
 * @author nathan
 * 
 */
public class HTMLBuilder {
	HTML html = new HTML();

	public Head head(final String title) {
		return html.head(title);
	}

	public Body body() {
		return html.body();
	}

	void build(final StringBuilder b) {
		html.display(b, 0);
	}

	String build() {
		StringBuilder b = new StringBuilder();
		build(b);
		return b.toString();
	}

	interface HTMLNode {
		void display(StringBuilder b, int depth);
	}

	interface BodyNode extends HTMLNode {
	}

	static class Text implements BodyNode {
		private final String text;

		public Text(final String text) {
			this.text = text;
		}

		public void display(final StringBuilder b, final int depth) {
			b.append(XMLUtil.escape(text));
		}
	}

	static class HTML implements HTMLNode {
		private Head head;
		private Body body;

		public void display(final StringBuilder b, final int depth) {
			b.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
			b.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");
			head.display(b, depth + 1);
			body.display(b, depth + 1);
			b.append("</html>");
		}

		public Head head(final String title) {
			head = new Head(title);
			return head;
		}

		public Body body() {
			body = new Body();
			return body;
		}
	}

	static class Head implements HTMLNode {
		private final String title;
		private final List<Link> links;
		private final List<Script> scripts;

		public Head(final String title) {
			this.title = title;
			links = new ArrayList<Link>();
			scripts = new ArrayList<Script>();
		}

		public Head script(final Script s) {
			scripts.add(s);
			return this;
		}

		public Head link(final Link l) {
			links.add(l);
			return this;
		}

		public Head javaScript(final String src) {
			return script(new Script("javascript", "text/javascript", src));
		}

		public Head javaScript(final String src, final boolean ieOnly) {
			return script(new Script("javascript", "text/javascript", src,
					ieOnly));
		}

		public Head styleSheet(final String href) {
			return link(new Link("stylesheet", "text/css", href));
		}

		public void display(final StringBuilder b, final int depth) {
			b.append("<head>");
			b.append(String.format("<title>%s</title>", title));
			for (Link l : links) {
				l.display(b, depth + 1);
			}
			for (Script s : scripts) {
				s.display(b, depth + 1);
			}
			b.append("</head>");
		}

	}

	static class Link implements HTMLNode {
		private final String href;
		private final String type;
		private final String rel;

		Link(final String rel, final String type, final String href) {
			this.rel = rel;
			this.type = type;
			this.href = href;
		}

		public void display(final StringBuilder b, final int depth) {
			b.append(String.format(
					"<link rel=\"%s\" type=\"%s\" href=\"%s\" />", rel, type,
					href));
		}

	}

	static class Script implements HTMLNode {
		private final String language;
		private final String type;
		private final String src;
		private final boolean ieOnly;

		public Script(final String language, final String type, final String src) {
			this(language, type, src, false);
		}

		public Script(final String language, final String type,
				final String src, final boolean ieOnly) {
			this.language = language;
			this.type = type;
			this.src = src;
			this.ieOnly = ieOnly;
		}

		public void display(final StringBuilder b, final int depth) {
			if (ieOnly) {
				b.append("<!--[if IE]>");
			}
			b.append(String.format(
					"<script language=\"%s\" type=\"%s\" src=\"%s\"></script>",
					language, type, src));
			if (ieOnly) {
				b.append("<![endif]-->");
			}
		}

	}

	abstract static class Container {
		private final List<BodyNode> nodes = new ArrayList<BodyNode>();
		private final List<String> classes = new ArrayList<String>();
		private String id;

		abstract String getContainerName();

		String additionalAttributes() {
			return null;
		}

		public Container clazz(final String clazz) {
			classes.add(clazz);
			return this;
		}

		public Container id(final String id) {
			this.id = id;
			return this;
		}

		public void display(final StringBuilder b, final int depth) {
			tag(b, getContainerName(), classes, id, additionalAttributes());
			for (BodyNode n : nodes) {
				n.display(b, depth + 1);
			}
			b.append(String.format("</%s>", getContainerName()));
		}

		public Div div() {
			return add(new Div());
		}

		public H h(final int level) {
			return add(new H(level));
		}

		public IMG img(final String src) {
			return add(new IMG(src));
		}

		public OL ol() {
			return add(new OL());
		}

		public UL ul() {
			return add(new UL());
		}

		public Span span() {
			return add(new Span());
		}

		public Table table() {
			return add(new Table());
		}

		public A a(final String href) {
			return add(new A(href));
		}

		public Text text(final String text) {
			return add(new Text(text));
		}

		public P p() {
			return add(new P());
		}

		public P p(final String text) {
			P p = p();
			p.text(text);
			return p;
		}

		public HR hr() {
			return add(new HR());
		}

		private <T extends BodyNode> T add(final T b) {
			nodes.add(b);
			return b;
		}
	}

	static class Body extends Container implements BodyNode {
		@Override
		String getContainerName() {
			return "body";
		}
	}

	static class Div extends Container implements BodyNode {
		@Override
		String getContainerName() {
			return "div";
		}
	}

	static class Span extends Container implements BodyNode {
		@Override
		String getContainerName() {
			return "span";
		}
	}

	public abstract static class HTMLList {
		abstract String getListName();

		private final List<String> classes = new ArrayList<String>();
		private String id;
		private final List<LI> items = new ArrayList<LI>();

		public HTMLList clazz(final String clazz) {
			classes.add(clazz);
			return this;
		}

		public HTMLList id(final String id) {
			this.id = id;
			return this;
		}

		public void display(final StringBuilder b, final int depth) {
			tag(b, getListName(), classes, id, null);
			for (LI item : items) {
				item.display(b, depth + 1);
			}
			b.append(String.format("</%s>", getListName()));
		}

		public LI li() {
			LI li = new LI();
			items.add(li);
			return li;
		}

	}

	static class UL extends HTMLList implements BodyNode {
		@Override
		String getListName() {
			return "ul";
		}
	}

	static class OL extends HTMLList implements BodyNode {
		@Override
		String getListName() {
			return "ol";
		}
	}

	static class LI extends Container {
		@Override
		String getContainerName() {
			return "li";
		}
	}

	static class H extends Container implements BodyNode {
		private final int level;

		H(final int level) {
			this.level = level;
		}

		@Override
		String getContainerName() {
			return "h" + level;
		}

	}

	static class IMG implements BodyNode {
		private final String src;
		private String alt;

		public IMG(final String src) {
			this.src = src;
		}

		public IMG alt(final String altText) {
			this.alt = altText;
			return this;
		}

		public void display(final StringBuilder b, final int depth) {
			b.append(String.format("<img src=\"%s\" alt=\"%s\" />", src, alt));
		}

	}

	static class Table implements BodyNode {
		private final List<String> classes = new ArrayList<String>();
		private String id;
		private Row header;
		private final List<Row> rows = new ArrayList<Row>();;
		private Row footer;

		public Table clazz(final String clazz) {
			classes.add(clazz);
			return this;
		}

		public Table id(final String id) {
			this.id = id;
			return this;
		}

		public Row header() {
			header = new Row();
			return header;
		}

		public Row footer() {
			footer = new Row();
			return footer;
		}

		public Row row() {
			Row r = new Row();
			rows.add(r);
			return r;
		}

		public void display(final StringBuilder b, final int depth) {
			tag(b, "table", classes, id, null);
			if (header != null) {
				b.append("<thead>");
				header.display(b, depth + 2);
				b.append("</thead>");
			}
			b.append("<tbody>");
			for (Row r : rows) {
				r.display(b, depth + 2);
			}
			b.append("</tbody>");
			if (footer != null) {
				b.append("<thead>");
				footer.display(b, depth + 2);
				b.append("</thead>");
			}
			b.append("</table>");
		}

	}

	static class Row implements HTMLNode {
		private final List<Col> cols = new ArrayList<Col>();

		public TD td() {
			TD td = new TD();
			cols.add(td);
			return td;
		}

		public Row td(final String col) {
			TD td = new TD();
			cols.add(td);
			td.text(col);
			return this;
		}

		public TH th() {
			TH th = new TH();
			cols.add(th);
			return th;
		}

		public Row th(final String col) {
			TH th = new TH();
			cols.add(th);
			th.text(col);
			return this;
		}

		public void display(final StringBuilder b, final int depth) {
			b.append("<tr>");
			for (Col c : cols) {
				c.display(b, depth + 2);
			}
			b.append("</tr>");
		}

	}

	interface Col extends HTMLNode {

	}

	static class TD extends Container implements Col {

		private int colspan;

		@Override
		String getContainerName() {
			return "td";
		}

		TD colspan(final int num) {
			this.colspan = num;
			return this;
		}

		@Override
		String additionalAttributes() {
			if (colspan > 0) {
				return String.format("colspan=%d", colspan);
			}
			return null;
		}
	}

	static class TH extends Container implements Col {

		@Override
		String getContainerName() {
			return "th";
		}

	}

	static class A extends Container implements BodyNode {
		private final String href;

		A(final String href) {
			this.href = href;
		}

		@Override
		String getContainerName() {
			return "a";
		}

		@Override
		protected String additionalAttributes() {
			return String.format("href=\"%s\"", href);
		}

	}

	static class P extends Container implements BodyNode {

		@Override
		String getContainerName() {
			return "p";
		}

	}

	static class HR implements BodyNode {

		public void display(final StringBuilder b, final int depth) {
			b.append("<hr/>");
		}

	}

	private static void tag(final StringBuilder b, final String name,
			final List<String> classes, final String id, final String attrs) {
		b.append('<');
		b.append(name);
		if (!classes.isEmpty()) {
			b.append(" class=\"");
			for (Iterator<String> iter = classes.iterator(); iter.hasNext();) {
				String clazz = iter.next();
				b.append(clazz);
				if (iter.hasNext()) {
					b.append(' ');
				}
			}
			b.append("\"");
		}
		if (id != null) {
			b.append(" id=\"");
			b.append(id);
			b.append("\"");
		}
		if (attrs != null) {
			b.append(' ');
			b.append(attrs);
		}
		b.append('>');
	}
}
