package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Roadmap;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 * The road itself: damage per second climbing against coins spent. Each upgrade is a
 * waypoint carrying its item; the stretch the player's coins already cover is lit gold,
 * the rest waits in the dusk. Coins run on a square-root scale so cheap early steps are
 * not crushed against the left edge by one expensive leap at the end.
 */
public class RoadChart extends JComponent
{
	private static final int HEIGHT = 160;
	private static final int PAD_LEFT = 8;
	private static final int PAD_RIGHT = 12;
	private static final int PAD_TOP = 26;
	private static final int PAD_BOTTOM = 22;
	private static final int NODE = 20;

	private final Roadmap map;
	private final List<BufferedImage> icons = new ArrayList<>();
	private int hover = -1;

	public RoadChart(Roadmap map, ItemIcons icons)
	{
		this.map = map;
		setPreferredSize(new Dimension(10, HEIGHT));
		setOpaque(false);
		for (Roadmap.Step step : map.getPath())
		{
			this.icons.add(icons == null ? null : icons.icon(step.getOption().getIcon().getId(), this::repaint));
		}
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				int index = nodeAt(e.getX(), e.getY());
				if (index != hover)
				{
					hover = index;
					setToolTipText(index < 0 ? null : tooltip(map.getPath().get(index)));
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = -1;
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	private static String tooltip(Roadmap.Step step)
	{
		Roadmap.Option o = step.getOption();
		return "<html><b>" + Ui.escape(o.getName()) + "</b><br>" + Format.gain(o.getGain()) + " DPS for " + Format.coins(o.getCost())
			+ "<br>" + Format.coins(step.getSpent()) + " spent by here</html>";
	}

	/**
	 * Where a coin total sits across the chart. A quarter of the distance follows the square root
	 * of the coins and the rest the step count, so dear steps late on still get room apart.
	 */
	private double x(long spent, double total, int w)
	{
		List<Roadmap.Step> path = map.getPath();
		int n = path.size();
		double t = 0;
		if (total > 0 && n > 0)
		{
			long before = 0;
			for (int k = 0; k < n; k++)
			{
				long after = path.get(k).getSpent();
				if (spent <= after || k == n - 1)
				{
					double a = blend(before, k, total, n);
					double b = blend(after, k + 1, total, n);
					double span = Math.sqrt(after / total) - Math.sqrt(before / total);
					double part = span <= 0 ? 1 : (Math.sqrt(Math.min(spent, after) / total) - Math.sqrt(before / total)) / span;
					t = a + (b - a) * Math.max(0, Math.min(1, part));
					break;
				}
				before = after;
			}
		}
		return PAD_LEFT + NODE / 2.0 + t * (w - PAD_LEFT - PAD_RIGHT - NODE);
	}

	private static double blend(long spent, int step, double total, int steps)
	{
		return 0.25 * Math.sqrt(spent / total) + 0.75 * step / steps;
	}

	private double y(double dps, double low, double high, int h)
	{
		double t = high <= low ? 0 : (dps - low) / (high - low);
		return h - PAD_BOTTOM - NODE / 2.0 - t * (h - PAD_TOP - PAD_BOTTOM - NODE);
	}

	private int nodeAt(int mx, int my)
	{
		List<Roadmap.Step> path = map.getPath();
		double total = path.isEmpty() ? 0 : path.get(path.size() - 1).getSpent();
		double low = map.getNow().dps();
		double high = map.finalDps();
		for (int i = 0; i < path.size(); i++)
		{
			double nx = x(path.get(i).getSpent(), total, getWidth());
			double ny = y(path.get(i).getOption().getAfter().dps(), low, high, getHeight());
			if (Math.abs(mx - nx) <= NODE / 2.0 + 1 && Math.abs(my - ny) <= NODE / 2.0 + 1)
			{
				return i;
			}
		}
		return -1;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		RoundRectangle2D frame = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, h, Theme.INK_DEEP));
		g.fill(frame);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(frame);

		List<Roadmap.Step> path = map.getPath();
		double now = map.getNow().dps();
		double end = map.finalDps();
		long total = path.isEmpty() ? 0 : path.get(path.size() - 1).getSpent();
		g.setClip(frame);

		// Faint contour lines, like a surveyor's map.
		g.setStroke(new BasicStroke(1f));
		for (int i = 1; i < 4; i++)
		{
			int gy = PAD_TOP + NODE / 2 + i * (h - PAD_TOP - PAD_BOTTOM - NODE) / 4;
			g.setColor(Theme.alpha(Theme.STROKE, 70));
			g.drawLine(PAD_LEFT, gy, w - PAD_RIGHT, gy);
		}

		int n = path.size();
		double[] xs = new double[n + 1];
		double[] ys = new double[n + 1];
		xs[0] = x(0, total, w);
		ys[0] = y(now, now, end, h);
		for (int i = 0; i < n; i++)
		{
			xs[i + 1] = x(path.get(i).getSpent(), total, w);
			ys[i + 1] = y(path.get(i).getOption().getAfter().dps(), now, end, h);
		}

		// Where the player's coins run out.
		double reach = map.getCoins() >= total ? xs[n] : x(map.getCoins(), total, w);
		if (n > 0)
		{
			Path2D area = smooth(xs, ys);
			area.lineTo(xs[n], h - PAD_BOTTOM + 4);
			area.lineTo(xs[0], h - PAD_BOTTOM + 4);
			area.closePath();
			g.setPaint(new GradientPaint(0, PAD_TOP, Theme.alpha(Theme.GOLD, 46), 0, h - PAD_BOTTOM, Theme.alpha(Theme.GOLD, 0)));
			g.fill(area);

			Path2D line = smooth(xs, ys);
			g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(Theme.alpha(Theme.INK_DEEP, 220));
			g.draw(line);
			g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 4f}, 0));
			g.setColor(Theme.alpha(Theme.GOLD_DIM, 170));
			g.draw(line);
			// The covered stretch is drawn solid over the dashed road.
			Graphics2D lit = (Graphics2D) g.create();
			lit.clipRect(0, 0, (int) Math.ceil(reach), h);
			lit.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			lit.setPaint(new GradientPaint((float) xs[0], 0, Theme.GOLD, (float) Math.max(xs[0] + 1, reach), 0, Theme.GOLD_BRIGHT));
			lit.draw(line);
			lit.dispose();

			if (map.getCoins() > 0 && map.getCoins() < total)
			{
				g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 3f}, 0));
				g.setColor(Theme.alpha(Theme.TEAL, 170));
				g.drawLine((int) reach, PAD_TOP - 6, (int) reach, h - PAD_BOTTOM + 2);
				g.setFont(Theme.small());
				FontMetrics fm = g.getFontMetrics();
				String label = "Your coins";
				int lx = (int) Math.min(w - PAD_RIGHT - fm.stringWidth(label), Math.max(PAD_LEFT, reach - fm.stringWidth(label) / 2.0));
				ProgressRing.drawShadowed(g, label, lx, h - 6, Theme.TEAL);
			}
		}

		// The start: where the player stands today.
		g.setColor(Theme.INK_DEEP);
		g.fill(new Ellipse2D.Double(xs[0] - 5, ys[0] - 5, 10, 10));
		g.setColor(Theme.GOLD);
		g.setStroke(new BasicStroke(1.6f));
		g.draw(new Ellipse2D.Double(xs[0] - 4, ys[0] - 4, 8, 8));

		for (int i = 0; i < n; i++)
		{
			drawNode(g, xs[i + 1], ys[i + 1], i, path.get(i).isAffordable());
		}

		g.setClip(null);
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		boolean coinsLabel = map.getCoins() > 0 && map.getCoins() < total;
		if (!coinsLabel || reach > PAD_LEFT + 90)
		{
			ProgressRing.drawShadowed(g, Format.dps(now) + " now", PAD_LEFT + 2, h - 6, Theme.TEXT_MUTED);
		}
		String top = n == 0 ? "Nothing left to buy" : Format.dps(end) + " DPS  ·  " + Format.coins(total);
		ProgressRing.drawShadowed(g, top, w - PAD_RIGHT - fm.stringWidth(top), 15, n == 0 ? Theme.TEXT_MUTED : Theme.GOLD_BRIGHT);
		ProgressRing.drawShadowed(g, "THE ROAD AHEAD", PAD_LEFT + 2, 15, Theme.GOLD);
		g.dispose();
	}

	private void drawNode(Graphics2D g, double cx, double cy, int index, boolean affordable)
	{
		float r = NODE / 2f;
		boolean lit = hover == index;
		Color rim = affordable ? Theme.GOLD : Theme.STROKE;
		g.setColor(Theme.alpha(Theme.INK_DEEP, 235));
		g.fill(new Ellipse2D.Double(cx - r, cy - r, NODE, NODE));
		if (affordable || lit)
		{
			g.setColor(Theme.alpha(lit ? Theme.GOLD_BRIGHT : Theme.GOLD, lit ? 70 : 38));
			g.fill(new Ellipse2D.Double(cx - r - 3, cy - r - 3, NODE + 6, NODE + 6));
		}
		g.setStroke(new BasicStroke(1.4f));
		g.setColor(lit ? Theme.GOLD_BRIGHT : rim);
		g.draw(new Ellipse2D.Double(cx - r, cy - r, NODE, NODE));
		BufferedImage image = icons.get(index);
		if (image != null)
		{
			double scale = Math.min((NODE - 4) / (double) Math.max(1, image.getWidth()), (NODE - 4) / (double) Math.max(1, image.getHeight()));
			int dw = (int) Math.round(image.getWidth() * scale);
			int dh = (int) Math.round(image.getHeight() * scale);
			Composite old = g.getComposite();
			if (!affordable && !lit)
			{
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
			}
			g.drawImage(image, (int) Math.round(cx - dw / 2.0), (int) Math.round(cy - dh / 2.0), dw, dh, null);
			g.setComposite(old);
		}
		// The step number sits on the node's shoulder.
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		String label = String.valueOf(index + 1);
		float bx = (float) (cx + r - 4);
		float by = (float) (cy - r - 3);
		g.setColor(Theme.INK_DEEP);
		g.fill(new Ellipse2D.Float(bx - 5, by - 5, 10, 10));
		g.setColor(affordable ? Theme.GOLD : Theme.TEXT_FAINT);
		g.draw(new Ellipse2D.Float(bx - 5, by - 5, 10, 10));
		ProgressRing.drawShadowed(g, label, Math.round(bx - fm.stringWidth(label) / 2f), Math.round(by + fm.getAscent() / 2f - 1),
			affordable ? Theme.GOLD_BRIGHT : Theme.TEXT_MUTED);
	}

	/** A gently curving road through the waypoints. */
	private static Path2D smooth(double[] xs, double[] ys)
	{
		Path2D path = new Path2D.Double();
		path.moveTo(xs[0], ys[0]);
		for (int i = 1; i < xs.length; i++)
		{
			double mid = (xs[i - 1] + xs[i]) / 2;
			path.curveTo(mid, ys[i - 1], mid, ys[i], xs[i], ys[i]);
		}
		return path;
	}
}
