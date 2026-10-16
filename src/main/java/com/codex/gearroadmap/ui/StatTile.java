package com.codex.gearroadmap.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** A small engraved tablet: a big number, its label, and a quiet line of context. */
class StatTile extends JComponent
{
	private final String value;
	private final String label;
	private final String note;
	private final Color color;

	StatTile(String value, String label, String note, Color color)
	{
		this.value = value;
		this.label = label;
		this.note = note;
		this.color = color;
		setPreferredSize(new Dimension(10, 54));
		setOpaque(false);
		setToolTipText(note == null ? label : label + ": " + note);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, h, Theme.CARD));
		g.fill(shape);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(shape);
		g.setColor(Theme.alpha(color, 160));
		g.fillRect(Theme.RADIUS, 1, w - Theme.RADIUS * 2, 1);

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(label.toUpperCase(), 8, 6 + small.getAscent());

		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		ProgressRing.drawShadowed(g, value, 8, 8 + small.getHeight() + bold.getAscent(), color);
		if (note != null)
		{
			g.setFont(Theme.small());
			String clipped = note;
			while (clipped.length() > 1 && small.stringWidth(clipped) > w - 16)
			{
				clipped = clipped.substring(0, clipped.length() - 2) + "…";
			}
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(clipped, 8, h - 6);
		}
		g.dispose();
	}
}
