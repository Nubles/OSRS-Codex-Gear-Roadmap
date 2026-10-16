package com.codex.gearroadmap.ui;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Gear Roadmap's mark: a gilt signpost over a winding road with a teal waypoint at its end. */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;

		// The road, winding up from the bottom left.
		Path2D road = new Path2D.Float();
		road.moveTo(x + 1 * s, y + 15.5f * s);
		road.curveTo(x + 6 * s, y + 14 * s, x + 2 * s, y + 10.5f * s, x + 7 * s, y + 10 * s);
		road.curveTo(x + 11 * s, y + 9.6f * s, x + 10 * s, y + 7.4f * s, x + 13.5f * s, y + 6.6f * s);
		g.setStroke(new BasicStroke(Math.max(1.6f, 2.2f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
		g.draw(road);
		g.setStroke(new BasicStroke(Math.max(1f, 1.3f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setPaint(new GradientPaint(x, y + 16 * s, Theme.GOLD_DIM, x + 14 * s, y + 6 * s, Theme.GOLD_BRIGHT));
		g.draw(road);

		// The post and its two boards.
		g.setStroke(new BasicStroke(Math.max(1f, 1.1f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.GOLD);
		g.drawLine(Math.round(x + 4.5f * s), Math.round(y + 2 * s), Math.round(x + 4.5f * s), Math.round(y + 11.5f * s));
		Path2D board = new Path2D.Float();
		board.moveTo(x + 4.5f * s, y + 2.2f * s);
		board.lineTo(x + 9.4f * s, y + 2.2f * s);
		board.lineTo(x + 10.8f * s, y + 3.4f * s);
		board.lineTo(x + 9.4f * s, y + 4.6f * s);
		board.lineTo(x + 4.5f * s, y + 4.6f * s);
		board.closePath();
		Path2D back = new Path2D.Float();
		back.moveTo(x + 4.5f * s, y + 5.6f * s);
		back.lineTo(x + 1.6f * s, y + 5.6f * s);
		back.lineTo(x + 0.5f * s, y + 6.7f * s);
		back.lineTo(x + 1.6f * s, y + 7.8f * s);
		back.lineTo(x + 4.5f * s, y + 7.8f * s);
		back.closePath();
		g.setPaint(new GradientPaint(x, y + 2 * s, Theme.GOLD_BRIGHT, x, y + 8 * s, Theme.GOLD_DIM));
		g.fill(board);
		g.fill(back);
		g.setColor(Theme.INK_DEEP);
		g.setStroke(new BasicStroke(Math.max(0.7f, 0.6f * s)));
		g.draw(board);
		g.draw(back);

		// The waypoint the road leads to.
		float r = 1.9f * s;
		g.setColor(Theme.alpha(Theme.TEAL, 90));
		g.fill(new Ellipse2D.Float(x + 13.5f * s - r * 1.6f, y + 6.6f * s - r * 1.6f, r * 3.2f, r * 3.2f));
		g.setColor(Theme.TEAL);
		g.fill(new Ellipse2D.Float(x + 13.5f * s - r, y + 6.6f * s - r, r * 2, r * 2));
	}
}
