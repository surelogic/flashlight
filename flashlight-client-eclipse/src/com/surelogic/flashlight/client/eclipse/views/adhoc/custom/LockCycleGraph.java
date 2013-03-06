package com.surelogic.flashlight.client.eclipse.views.adhoc.custom;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.PageBook;
import org.jdesktop.core.animation.timing.TimingSource;
import org.jdesktop.swt.animation.timing.sources.SWTTimingSource;

import com.surelogic.NonNull;
import com.surelogic.common.SLUtility;
import com.surelogic.common.graph.Edge;
import com.surelogic.common.graph.Graph;
import com.surelogic.common.graph.Node;
import com.surelogic.common.ui.EclipseColorUtility;
import com.surelogic.common.ui.adhoc.AbstractQueryResultCustomDisplay;

public final class LockCycleGraph extends AbstractQueryResultCustomDisplay {

	private SWTTimingSource f_ts;
	private Graph f_graph;

	@Override
	public void dispose() {
		System.out.println("dispose() called");
		if (f_ts != null) {
			f_ts.dispose();
			f_ts = null;
		}
	}

	@Override
	protected void displayResult(Composite panel) {
		final SashForm sash = new SashForm(panel, SWT.HORIZONTAL | SWT.SMOOTH);
		sash.setLayout(new FillLayout());

		Graph.Builder b = new Graph.Builder();
		b.addEdge("Object-1", "AWT-edge-3456");
		b.addEdge("AWT-edge-3456", "Object-1");
		b.addEdge("AWT-edge-3456", "Object-2");
		b.addEdge("Object-2", "AWT-edge-3456");
		b.addEdge("Object-2", "Object-1");
		b.addEdge("Object-1", "Object-2");
		f_graph = b.build();
		f_graph.transform(150, 150);

		/*
		 * Left-hand-side shows graph.
		 */
		final Canvas lhs = new Canvas(sash, SWT.DOUBLE_BUFFERED
				| SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
		lhs.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				final Rectangle clientArea = lhs.getClientArea();
				e.gc.setAntialias(SWT.ON);
				e.gc.setBackground(EclipseColorUtility
						.getSlightlyDarkerBackgroundColor());
				e.gc.fillRectangle(0, 0, clientArea.width, clientArea.height);

				for (Edge edge : f_graph.edges()) {
					drawEdge(e.gc, edge.getFrom(), edge.getTo());
				}
				for (Node node : f_graph.nodes()) {
					drawNode(e.gc, node);
				}
			}
		});

		/*
		 * Right-hand-side
		 */
		final PageBook rhs = new PageBook(sash, SWT.NONE);

		final Label noSelectionPane = new Label(rhs, SWT.NONE);
		rhs.showPage(noSelectionPane);

		f_ts = new SWTTimingSource(lhs.getDisplay());
		f_ts.addTickListener(new TimingSource.TickListener() {

			@Override
			public void timingSourceTick(TimingSource source, long nanoTime) {
				f_graph.relax();
				lhs.redraw();
			}
		});
		f_ts.init();
	}

	private static final int PAD = 5;
	private static final int SIZE_ADJUST = 2;
	private static final int CTRL_DIST = 35;
	private static final int ARROW_DIST = 10;

	private void drawNode(final GC gc, final Node node) {
		final Rectangle nr = getCenteredRectangleFor(gc, node);

		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRoundRectangle(nr.x, nr.y, nr.width, nr.height, PAD, PAD);
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRoundRectangle(nr.x + 1, nr.y + 1, nr.width - 1, nr.height - 1,
				PAD, PAD);

		gc.setForeground(EclipseColorUtility.getSubtleTextColor());
		gc.drawText(node.getLabel(), nr.x + PAD, nr.y + PAD,
				SWT.DRAW_TRANSPARENT);
	}

	private void drawEdge(final GC gc, final Node from, final Node to) {
		final Point cFrom = getCenterOf(from);
		final Point cTo = getCenterOf(to);

		final Point mid = getMidPointBetween(cFrom, cTo);
		gc.drawRectangle(mid.x - 5, mid.y - 5, 10, 10);
		int dx = cTo.x - cFrom.x;
		int dy = cTo.y - cFrom.y;

		final Point ctrl = getPointPerpendicularToMidPointOfLine(cFrom, cTo,
				CTRL_DIST);

		final Path path = new Path(gc.getDevice());
		path.moveTo(cFrom.x, cFrom.y);
		path.quadTo(ctrl.x, ctrl.y, cTo.x, cTo.y);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		gc.drawOval(ctrl.x - 5, ctrl.y - 5, 10, 10);
		gc.drawPath(path);

		final Point midPath = getMidPointBetween(ctrl, mid);
		final Point arrow1 = getPointPerpendicularToMidPointOfLine(ctrl,
				midPath, ARROW_DIST);
		final Point arrow2 = getPointPerpendicularToMidPointOfLine(midPath,
				mid, ARROW_DIST);
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		gc.fillPolygon(new int[] { midPath.x, midPath.y, arrow1.x, arrow1.y,
				arrow2.x, arrow2.y });
	}

	@NonNull
	private Rectangle getCenteredRectangleFor(@NonNull GC gc, @NonNull Node node) {
		final Point extent = gc.textExtent(node.getLabel());
		final Point c = getCenterOf(node);

		final int xt = c.x - (extent.x / 2);
		final int yt = c.y - (extent.y / 2);

		int xb = xt - PAD;
		int yb = yt - PAD;
		int wb = extent.x + PAD + PAD;
		int hb = extent.y + PAD + PAD;

		return new Rectangle(xb, yb, wb - SIZE_ADJUST, hb - SIZE_ADJUST);
	}

	private Point getExitPointFor(@NonNull GC gc, @NonNull Node node,
			Point outside) {
		final Rectangle nr = getCenteredRectangleFor(gc, node);
		final Point c = getCenterOf(node);
		double m = getSlope(c, outside);
		double b = getYIntercept(c, outside);

		final int xRec = (c.x > outside.x) ? nr.x /* left */: nr.x + nr.width /* right */;
		final int yRec = (c.y > outside.y) ? nr.y /* top */: nr.y + nr.height /* bottom */;

		/*
		 * Check special case where x=x
		 */
		if (c.x == outside.x) {
			return new Point(c.x, yRec);
		}

		/*
		 * Check special case where y=y
		 */
		if (c.y == outside.y) {
			return new Point(xRec, c.y);
		}

		int y = getYOnLine(m, b, xRec);
		if (isYinBox(y, nr))
			return new Point(xRec, y);
		else
			return new Point(getXOnLine(m, b, yRec), yRec);
	}

	private static boolean isYinBox(int y, Rectangle r) {
		return r.y <= y && y <= (r.y + r.height);
	}

	private static double getSlope(Point p1, Point p2) {
		final double slope = (double) (p2.y - p1.y) / (double) (p2.x - p1.x);
		return slope;
	}

	private static double getYIntercept(Point p1, Point p2) {
		final double slope = getSlope(p1, p2);
		final double yIntercept = -slope * (double) p2.x + (double) p2.y;
		return yIntercept;
	}

	private static double getYIntercept(double slope, Point p2) {
		final double yIntercept = -slope * (double) p2.x + (double) p2.y;
		return yIntercept;
	}

	private static int getYOnLine(double slope, double yIntercept, int x) {
		double y = slope * (double) x + yIntercept;
		return SLUtility.safeLongToInt(Math.round(y));
	}

	private static int getXOnLine(double slope, double yIntercept, int y) {
		double x = ((double) y - yIntercept) / slope;
		return SLUtility.safeLongToInt(Math.round(x));
	}

	private static Point getMidPointBetween(Point p1, Point p2) {
		double hx = (p1.x - p2.x) / 2.0;
		double hy = (p1.y - p2.y) / 2.0;
		return new Point(p2.x + SLUtility.safeLongToInt(Math.round(hx)), p2.y
				+ SLUtility.safeLongToInt(Math.round(hy)));
	}

	private static Point getCenterOf(@NonNull Node node) {
		final int x = SLUtility.safeLongToInt(Math.round(node.getX()));
		final int y = SLUtility.safeLongToInt(Math.round(node.getY()));
		return new Point(x, y);
	}

	private static Point getPointPerpendicularToMidPointOfLine(Point p1,
			Point p2, double distance) {
		int dx = p2.x - p1.x;
		final Point mid = getMidPointBetween(p1, p2);
		final double slope = getSlope(p1, p2);
		final double tslope = -1.0 / slope;
		final double yInt = getYIntercept(tslope, mid);

		double v = distance / Math.sqrt(1.0 + tslope * tslope);
		int mx = SLUtility.safeLongToInt(Math.round(v));
		int x = mid.x;
		if (dx < 0)
			x -= mx;
		else
			x += mx;
		final int y = getYOnLine(tslope, yInt, x);

		Point result = new Point(x, y);
		return result;
	}

	private static double getDistance(Point p1, Point p2) {
		final double vx = p2.x - p1.x;
		final double vy = p2.y - p1.y;
		double result = Math.sqrt(vx * vx + vy * vy);
		result = (result <= 0) ? .0001 : result;
		return result;
	}

	private static int getDistanceAsInt(Point p1, Point p2) {
		return SLUtility.safeLongToInt(Math.round(getDistance(p1, p2)));
	}
}