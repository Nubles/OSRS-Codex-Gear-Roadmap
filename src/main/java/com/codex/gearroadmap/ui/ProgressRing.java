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

/** A gilded progress ring with the percentage set in the game's bold font. */
public class ProgressRing extends JComponent
{
	private final int diameter;
	private final float thickness;
	private final Animator animator = new Animator(this);
	private String caption;
	private boolean complete;

	public ProgressRing(int diameter, float thickness)
	{
		this.diameter = diameter;
		this.thickness = thickness;
		Dimension size = new Dimension(diameter, diameter);
		setPreferredSize(size);
		setMinimumSize(size);
		setMaximumSize(size);
		setOpaque(false);
	}

	public void setProgress(double progress, boolean complete, boolean animate)
	{
		this.complete = complete;
		animator.set(Math.max(0, Math.min(1, progress)), animate);
	}

	/** A small line under the percentage, for rings large enough to hold it. */
	public void setCaption(String caption)
	{
		this.caption = caption;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		double value = animator.get();
		float inset = thickness / 2f + 1;
		float size = diameter - inset * 2;

		// Inner well: a faint radial glow tinted by progress.
		Color glow = complete ? Theme.GREEN : Theme.progressColor(value);
		float r = size / 2f;
		g.setPaint(new RadialGradientPaint(diameter / 2f, diameter / 2f, r,
			new float[]{0f, 1f}, new Color[]{Theme.alpha(glow, 40), Theme.alpha(Theme.INK_DEEP, 0)}));
		g.fill(new Ellipse2D.Float(inset, inset, size, size));

		g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Theme.STROKE, 200));
		g.draw(new Arc2D.Float(inset, inset, size, size, 0, 360, Arc2D.OPEN));

		if (value > 0)
		{
			g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			// Draw the arc in short segments so its colour can travel from ember to teal.
			int segments = Math.max(1, (int) Math.ceil(value * 72));
			double sweep = value * 360 / segments;
			for (int i = 0; i < segments; i++)
			{
				double t = (i + 1) / (double) segments * value;
				g.setColor(complete ? Theme.GREEN : Theme.progressColor(t));
				g.draw(new Arc2D.Double(inset, inset, size, size, 90 - i * sweep, -sweep - (i < segments - 1 ? 0.6 : 0), Arc2D.OPEN));
			}
		}

		// Fine gold rim outside the track.
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 90));
		g.draw(new Ellipse2D.Float(0.5f, 0.5f, diameter - 1, diameter - 1));

		g.setFont(diameter >= 48 ? Theme.title() : Theme.small());
		String text = Math.round(value * 100) + "%";
		FontMetrics fm = g.getFontMetrics();
		if (complete)
		{
			drawCheck(g, diameter / 2f, diameter / 2f - (caption != null && diameter >= 60 ? 5 : 0), diameter * 0.16f);
			text = null;
		}
		int textY = diameter / 2 + fm.getAscent() / 2 - 2;
		if (caption != null && diameter >= 60)
		{
			textY -= 5;
		}
		if (text != null)
		{
			drawShadowed(g, text, (diameter - fm.stringWidth(text)) / 2, textY, Theme.TEXT);
		}

		if (caption != null && diameter >= 60)
		{
			g.setFont(Theme.small());
			FontMetrics small = g.getFontMetrics();
			drawShadowed(g, caption, (diameter - small.stringWidth(caption)) / 2, textY + small.getAscent() + 1, Theme.TEXT_MUTED);
		}
		g.dispose();
	}

	/** A drawn check mark, since the game fonts have no glyph for one. */
	static void drawCheck(Graphics2D g, float cx, float cy, float r)
	{
		java.awt.geom.Path2D check = new java.awt.geom.Path2D.Float();
		check.moveTo(cx - r, cy);
		check.lineTo(cx - r * 0.3f, cy + r * 0.7f);
		check.lineTo(cx + r, cy - r * 0.65f);
		g.setStroke(new BasicStroke(Math.max(1.6f, r * 0.32f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.GREEN);
		g.draw(check);
	}

	static void drawShadowed(Graphics2D g, String text, int x, int y, Color color)
	{
		g.setColor(Color.BLACK);
		g.drawString(text, x + 1, y + 1);
		g.setColor(color);
		g.drawString(text, x, y);
	}
}
