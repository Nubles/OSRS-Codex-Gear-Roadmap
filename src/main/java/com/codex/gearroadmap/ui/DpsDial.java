package com.codex.gearroadmap.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;

/**
 * The crest's dial: today's damage per second in the middle, and an arc showing how far
 * along the road that is, from nothing to the damage at the road's end. A faint second
 * arc marks what the player's coins already reach.
 */
public class DpsDial extends JComponent
{
	private final int diameter;
	private final Animator now = new Animator(this);
	private final Animator reach = new Animator(this);
	private String value = "-";
	private Color color = Theme.GOLD;

	public DpsDial(int diameter)
	{
		this.diameter = diameter;
		Dimension d = new Dimension(diameter, diameter);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
		setOpaque(false);
	}

	/**
	 * @param share today's damage as a share of the road's end
	 * @param affordable the damage the player's coins reach, as the same share
	 */
	public void set(String value, double share, double affordable, Color color, boolean animate)
	{
		this.value = value;
		this.color = color;
		now.set(clamp(share), animate);
		reach.set(clamp(Math.max(share, affordable)), animate);
	}

	private static double clamp(double v)
	{
		return Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		float thickness = 6f;
		float inset = thickness / 2 + 2;
		float size = diameter - inset * 2;

		g.setPaint(new RadialGradientPaint(diameter / 2f, diameter / 2f, size / 2f, new float[]{0f, 1f},
			new Color[]{Theme.alpha(color, 46), Theme.alpha(Theme.INK_DEEP, 0)}));
		g.fill(new Ellipse2D.Float(inset, inset, size, size));

		// A 270 degree track open at the bottom, like a gauge.
		float start = 225;
		float sweep = -270;
		g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Theme.STROKE, 210));
		g.draw(new Arc2D.Float(inset, inset, size, size, start, sweep, Arc2D.OPEN));
		double reached = reach.get();
		if (reached > now.get() + 0.005)
		{
			g.setColor(Theme.alpha(Theme.GOLD, 70));
			g.draw(new Arc2D.Float(inset, inset, size, size, start, (float) (sweep * reached), Arc2D.OPEN));
		}
		double value = now.get();
		if (value > 0)
		{
			int segments = Math.max(1, (int) Math.ceil(value * 60));
			double step = sweep * value / segments;
			for (int i = 0; i < segments; i++)
			{
				double t = (i + 1) / (double) segments * value;
				g.setColor(Theme.mix(Theme.GOLD_DIM, color, Math.min(1, t * 1.3)));
				g.draw(new Arc2D.Double(inset, inset, size, size, start + i * step, step - (i < segments - 1 ? 0.6 : 0), Arc2D.OPEN));
			}
		}
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 90));
		g.draw(new Ellipse2D.Float(0.5f, 0.5f, diameter - 1, diameter - 1));

		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		int y = diameter / 2 + bold.getAscent() / 2 - 4;
		ProgressRing.drawShadowed(g, this.value, (diameter - bold.stringWidth(this.value)) / 2, y, Theme.TEXT);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		ProgressRing.drawShadowed(g, "DPS", (diameter - small.stringWidth("DPS")) / 2, y + small.getAscent() + 1, Theme.TEXT_MUTED);
		g.dispose();
	}
}
