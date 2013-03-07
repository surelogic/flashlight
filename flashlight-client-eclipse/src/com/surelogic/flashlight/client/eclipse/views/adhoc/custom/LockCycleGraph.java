package com.surelogic.flashlight.client.eclipse.views.adhoc.custom;

import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.part.PageBook;
import org.jdesktop.core.animation.timing.TimingSource;
import org.jdesktop.core.animation.timing.sources.ScheduledExecutorTimingSource;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.ThreadConfined;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.graph.Edge;
import com.surelogic.common.graph.Graph;
import com.surelogic.common.graph.Node;
import com.surelogic.common.ui.EclipseColorUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.adhoc.AbstractQueryResultCustomDisplay;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;

public final class LockCycleGraph extends AbstractQueryResultCustomDisplay {

  private TimingSource f_ts;
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
    final Canvas lhs = new Canvas(sash, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
    final CanvasEventHandler handler = new CanvasEventHandler(lhs, f_graph);
    lhs.addPaintListener(handler);
    lhs.addMouseListener(handler);

    /*
     * Right-hand-side
     */
    final PageBook rhs = new PageBook(sash, SWT.NONE);

    final Label noSelectionPane = new Label(rhs, SWT.NONE);
    rhs.showPage(noSelectionPane);

    f_ts = new ScheduledExecutorTimingSource(50, TimeUnit.MILLISECONDS);
    f_ts.addTickListener(handler);
    f_ts.init();

    sash.setWeights(new int[] { EclipseUtility.getIntPreference(FlashlightPreferencesUtility.DEADLOCK_GRAPH_SASH_LHS_WEIGHT),
        EclipseUtility.getIntPreference(FlashlightPreferencesUtility.DEADLOCK_GRAPH_SASH_RHS_WEIGHT) });

    /*
     * When the left-hand-side composite is resized we'll just guess that the
     * sash is involved. Hopefully, this is conservative. This seems to be the
     * only way to do this.
     */
    lhs.addListener(SWT.Resize, new Listener() {
      @Override
      public void handleEvent(final Event event) {
        final int[] weights = sash.getWeights();
        if (weights != null && weights.length == 2) {
          EclipseUtility.setIntPreference(FlashlightPreferencesUtility.DEADLOCK_GRAPH_SASH_LHS_WEIGHT, weights[0]);
          EclipseUtility.setIntPreference(FlashlightPreferencesUtility.DEADLOCK_GRAPH_SASH_RHS_WEIGHT, weights[1]);
        }
      }
    });
  }

  /*
   * This class handles events for the graph.
   */
  private static final class CanvasEventHandler extends MouseAdapter implements MouseMoveListener, PaintListener,
      TimingSource.TickListener {

    private final Canvas f_canvas;
    private final Image f_lock = SLImages.getImage(CommonImages.IMG_LOCK);
    private final Graph f_graph;

    private volatile boolean f_tracking = false;

    @ThreadConfined
    private Node f_trackingNode = null;
    @ThreadConfined
    private int f_x, f_y;

    public CanvasEventHandler(Canvas canvas, Graph graph) {
      f_canvas = canvas;
      f_graph = graph;
    }

    @Override
    public void mouseDown(MouseEvent e) {
      if (e.button == 1) {
        final Node node = getNodeOrNull(e.x, e.y, f_graph);
        if (node != null) {
          node.setPostitionFixed(true);
          f_tracking = true;
          f_trackingNode = node;
          f_x = e.x;
          f_y = e.y;
          f_canvas.addMouseMoveListener(this);
        }
      }
    }

    @Override
    public void mouseUp(MouseEvent e) {
      if (f_tracking) {
        f_tracking = false;
        f_trackingNode = null;
        f_x = f_y = 0;
        f_canvas.removeMouseMoveListener(this);
      }
    }

    @Override
    public void mouseMove(MouseEvent e) {
      final int dx = e.x - f_x;
      final int dy = e.y - f_y;
      f_x = e.x;
      f_y = e.y;
      f_trackingNode.movePosition(dx, dy);
    }

    @Override
    public void paintControl(PaintEvent e) {
      final Rectangle clientArea = f_canvas.getClientArea();
      e.gc.setAntialias(SWT.ON);
      e.gc.setBackground(EclipseColorUtility.getSlightlyDarkerBackgroundColor());
      e.gc.fillRectangle(0, 0, clientArea.width, clientArea.height);

      synchronized (f_graph) {
        for (Edge edge : f_graph.edges()) {
          drawEdge(e.gc, edge.getFrom(), edge.getTo(), false);
        }
        for (Node node : f_graph.nodes()) {
          drawNode(e.gc, node, false, node == f_trackingNode);
        }
      }
    }

    void drawEdge(final GC gc, final Node from, final Node to, boolean highlight) {
      final Point cFrom = getCenterOf(from);
      final Point cTo = getCenterOf(to);

      final Point mid = getMidPointBetween(cFrom, cTo);

      final Point ctrl = getPointPerpendicularToMidPointOfLine(cFrom, cTo, CTRL_DIST, true);

      final Path path = new Path(gc.getDevice());
      path.moveTo(cFrom.x, cFrom.y);
      path.quadTo(ctrl.x, ctrl.y, cTo.x, cTo.y);
      gc.setForeground(highlight ? EclipseColorUtility.getDiffHighlightColorNewChanged() : gc.getDevice().getSystemColor(
          SWT.COLOR_LIST_BACKGROUND));
      if (highlight) {
        final int saved = gc.getLineWidth();
        gc.setLineWidth(3);
        gc.drawPath(path);
        gc.setLineWidth(saved);
      } else
        gc.drawPath(path);

      if (DEBUG_DRAWING) {
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        gc.drawOval(ctrl.x - 5, ctrl.y - 5, 10, 10);
        gc.drawRectangle(mid.x - 5, mid.y - 5, 10, 10);
      }

      final Point midPath = getMidPointBetween(ctrl, mid);
      final Point arrow1 = getPointPerpendicularToMidPointOfLine(ctrl, midPath, ARROW_DIST, true);
      final Point arrow2 = getPointPerpendicularToMidPointOfLine(midPath, mid, ARROW_DIST, true);
      gc.setBackground(highlight ? EclipseColorUtility.getDiffHighlightColorNewChanged() : gc.getDevice().getSystemColor(
          SWT.COLOR_LIST_BACKGROUND));
      final int[] arrow = new int[] { midPath.x, midPath.y, arrow1.x, arrow1.y, arrow2.x, arrow2.y };
      gc.fillPolygon(arrow);
      gc.setForeground(EclipseColorUtility.getSubtleTextColor());
      gc.drawPolygon(arrow);
    }

    void drawNode(final GC gc, final Node node, boolean highlight, boolean select) {
      final Rectangle nr = getCenteredRectangleFor(gc, node);
      node.setData(nr);

      final Color bg, fg;
      if (select) {
        bg = gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION);
        fg = gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
      } else if (highlight) {
        bg = EclipseColorUtility.getDiffHighlightColorNewChanged();
        fg = gc.getDevice().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
      } else {
        bg = gc.getDevice().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        fg = gc.getDevice().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
      }

      gc.setForeground(EclipseColorUtility.getSubtleTextColor());
      gc.drawRoundRectangle(nr.x, nr.y, nr.width, nr.height, PAD, PAD);
      gc.setBackground(bg);
      gc.fillRoundRectangle(nr.x + 1, nr.y + 1, nr.width - 1, nr.height - 1, PAD, PAD);

      gc.setForeground(fg);
      gc.drawText(node.getLabel(), nr.x + LOCK_ICON_WIDTH + PAD, nr.y + PAD, SWT.DRAW_TRANSPARENT);

      gc.drawImage(f_lock, nr.x + PAD, nr.y + (nr.height / 2 - LOCK_ICON_WIDTH / 2));
    }

    @Override
    public void timingSourceTick(TimingSource source, long nanoTime) {
      // not in the SWT thread
      if (!f_tracking)
        f_graph.relax();

      f_canvas.getDisplay().asyncExec(new Runnable() {
        @Override
        public void run() {
          if (!f_canvas.isDisposed())
            f_canvas.redraw();
        }
      });
    }
  }

  /*
   * Static methods (many should be moved to common)
   */
  private static final boolean DEBUG_DRAWING = true;
  private static final int PAD = 5;
  private static final int SIZE_ADJUST = 2;
  private static final int LOCK_ICON_WIDTH = 16;
  private static final int CTRL_DIST = 35;
  private static final int ARROW_DIST = 10;

  @Nullable
  static Node getNodeOrNull(int x, int y, Graph graph) {
    synchronized (graph) {
      for (Node n : graph.nodes()) {
        final Object data = n.getData();
        if (data instanceof Rectangle) {
          final Rectangle bb = (Rectangle) data;
          if (bb.contains(x, y))
            return n;
        }
      }
    }
    return null;
  }

  @NonNull
  static Rectangle getCenteredRectangleFor(@NonNull GC gc, @NonNull Node node) {
    final Point extent = gc.textExtent(node.getLabel());
    final Point c = getCenterOf(node);

    final int xt = c.x - (extent.x / 2);
    final int yt = c.y - (extent.y / 2);

    int xb = xt - PAD;
    int yb = yt - PAD;
    int wb = extent.x + PAD + PAD;
    int hb = extent.y + PAD + PAD;

    return new Rectangle(xb, yb, wb + LOCK_ICON_WIDTH - SIZE_ADJUST, hb - SIZE_ADJUST);
  }

  static Point getExitPointFor(@NonNull GC gc, @NonNull Node node, Point outside) {
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

    int y = SLUtility.safeDoubleToInt(getYOnLine(m, b, xRec));
    if (isYinBox(y, nr))
      return new Point(xRec, y);
    else
      return new Point(getXOnLine(m, b, yRec), yRec);
  }

  static boolean isYinBox(int y, Rectangle r) {
    return r.y <= y && y <= (r.y + r.height);
  }

  static double getSlope(Point p1, Point p2) {
    final double slope = (double) (p2.y - p1.y) / (double) (p2.x - p1.x);
    return slope;
  }

  static double getYIntercept(Point p1, Point p2) {
    final double slope = getSlope(p1, p2);
    final double yIntercept = -slope * (double) p2.x + (double) p2.y;
    return yIntercept;
  }

  static double getYIntercept(double slope, Point p2) {
    final double yIntercept = -slope * (double) p2.x + (double) p2.y;
    return yIntercept;
  }

  static double getYOnLine(double slope, double yIntercept, double x) {
    double y = slope * x + yIntercept;
    return y;
  }

  static int getXOnLine(double slope, double yIntercept, int y) {
    double x = ((double) y - yIntercept) / slope;
    return SLUtility.safeDoubleToInt(x);
  }

  static Point getMidPointBetween(Point p1, Point p2) {
    double hx = (p1.x - p2.x) / 2.0;
    double hy = (p1.y - p2.y) / 2.0;
    return new Point(p2.x + SLUtility.safeDoubleToInt(hx), p2.y + SLUtility.safeDoubleToInt(hy));
  }

  static Point getCenterOf(@NonNull Node node) {
    final int x = SLUtility.safeDoubleToInt(node.getX());
    final int y = SLUtility.safeDoubleToInt(node.getY());
    return new Point(x, y);
  }

  /**
   * Determines a point perpendicular to the passed line, at the midpoint, a set
   * distance from the line. The point is either on the right or left, facing
   * the direction of the vector defined by the passed points.
   * 
   * @param from
   *          beginning of the line.
   * @param to
   *          end of the line.
   * @param distance
   *          in pixels away from the line.
   * @param toRight
   *          {@code true} to the right of the passed line, facing the direction
   *          of the vector, {@code false} to the left.
   * @return a point perpendicular to the passed line, at the midpoint, a set
   *         distance from the line.
   */
  static Point getPointPerpendicularToMidPointOfLine(Point from, Point to, double distance, boolean toRight) {
    final int dx = to.x - from.x;
    final int dy = to.y - from.y;
    final Point mid = getMidPointBetween(from, to);
    if (-5 < dx && dx < 5) { // slope is infinite
      final double val;
      if (dy > 0) { // pointing straight down
        val = subIfToRightOrAdd(mid.x, distance, toRight);
      } else { // pointing straight up
        val = addIfToRightOrSub(mid.x, distance, toRight);
      }
      return new Point(SLUtility.safeDoubleToInt(val), mid.y);
    } else if (-5 < dy && dy < 5) { // slope is zero
      final double val;
      if (dx > 0) { // pointing right
        val = subIfToRightOrAdd(mid.y, distance, toRight);
      } else { // pointing left
        val = addIfToRightOrSub(mid.y, distance, toRight);
      }
      return new Point(mid.x, SLUtility.safeDoubleToInt(val));
    } else {
      final double slope = getSlope(from, to);
      final double tslope = -1.0 / slope;
      final double yInt = getYIntercept(tslope, mid);

      final double v = distance / Math.sqrt(1.0 + tslope * tslope);
      final double x;
      if (dx > 0) {
        if (dy > 0) { // down-toward-right
          x = subIfToRightOrAdd(mid.x, v, toRight);
        } else { // up-toward-right
          x = addIfToRightOrSub(mid.x, v, toRight);
        }
      } else {
        if (dy > 0) { // down-toward-left
          x = subIfToRightOrAdd(mid.x, v, toRight);
        } else { // up-toward-left
          x = addIfToRightOrSub(mid.x, v, toRight);
        }
      }
      final double y = getYOnLine(tslope, yInt, x);

      Point result = new Point(SLUtility.safeDoubleToInt(x), SLUtility.safeDoubleToInt(y));
      return result;
    }
  }

  static double addIfToRightOrSub(double v1, double v2, boolean toRight) {
    return toRight ? v1 + v2 : v1 - v2;
  }

  static double subIfToRightOrAdd(double v1, double v2, boolean toRight) {
    return toRight ? v1 - v2 : v1 + v2;
  }

  static Point getPointPerpendicularToMidPointOfLineOLD(Point p1, Point p2, double distance) {
    final int dx = p2.x - p1.x;
    final int dy = p2.y - p1.y;
    final Point mid = getMidPointBetween(p1, p2);
    if (-5 < dx && dx < 5) { // slope is infinite
      final double val;
      if (dy < 0)
        val = mid.x - distance;
      else
        val = mid.x + distance;
      return new Point(SLUtility.safeDoubleToInt(val), mid.y);
    } else if (-5 < dy && dy < 5) { // slope is zero
      final double val;
      if (dx < 0)
        val = mid.y - distance;
      else
        val = mid.y + distance;
      return new Point(mid.x, SLUtility.safeDoubleToInt(val));
    } else {
      final double slope = getSlope(p1, p2);
      final double tslope = -1.0 / slope;
      final double yInt = getYIntercept(tslope, mid);

      double v = distance / Math.sqrt(1.0 + tslope * tslope);
      int mx = SLUtility.safeDoubleToInt(v);
      double x = mid.x;
      if (dx < 0)
        x -= mx;
      else
        x += mx;
      final double y = getYOnLine(tslope, yInt, x);

      Point result = new Point(SLUtility.safeDoubleToInt(x), SLUtility.safeDoubleToInt(y));
      return result;
    }
  }

  static double getDistance(Point p1, Point p2) {
    final double vx = p2.x - p1.x;
    final double vy = p2.y - p1.y;
    double result = Math.sqrt(vx * vx + vy * vy);
    result = (result <= 0) ? .0001 : result;
    return result;
  }

  static int getDistanceAsInt(Point p1, Point p2) {
    return SLUtility.safeDoubleToInt(getDistance(p1, p2));
  }
}