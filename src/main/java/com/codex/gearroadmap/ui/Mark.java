package com.codex.gearroadmap.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/** A drawn verdict: a sealed check, a warning, a cross, a quiet note, or an empty dashed ring. */
public class Mark extends JComponent
{
	public enum Kind
	{
		GOOD,
		NOTE,
		WARN,
		BAD,
		NONE
	}

	private final Kind kind;
	private final int size;

	public Mark(Kind kind, int size)
	{
		this.kind = kind;
		this.size = size;
		Dimension d = new Dimension(size, size);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
		setOpaque(false);
	}

	public static Color color(Kind kind)
	{
		switch (kind)
		{
			case GOOD:
				return Theme.GREEN;
			case WARN:
				return Theme.EMBER;
			case BAD:
				return Theme.BLOOD;
			case NOTE:
				return Theme.GOLD;
			default:
				return Theme.TEXT_FAINT;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		paint(g, kind, 0, 0, size);
		g.dispose();
	}

	/** Draws a mark anywhere, so the overlay can use the same shapes as the panel. */
	public static void paint(Graphics2D g, Kind kind, float x, float y, int size)
	{
		float s = size - 2;
		float o = 1;
		Color c = color(kind);
		Ellipse2D circle = new Ellipse2D.Float(x + o, y + o, s, s);
		g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		switch (kind)
		{
			case GOOD:
			{
				g.setColor(Theme.alpha(c, 60));
				g.fill(circle);
				g.setColor(c);
				g.draw(circle);
				Path2D check = new Path2D.Float();
				check.moveTo(x + o + s * 0.28, y + o + s * 0.52);
				check.lineTo(x + o + s * 0.44, y + o + s * 0.68);
				check.lineTo(x + o + s * 0.74, y + o + s * 0.34);
				g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(check);
				break;
			}
			case WARN:
			{
				Path2D tri = new Path2D.Float();
				tri.moveTo(x + o + s / 2, y + o + s * 0.04);
				tri.lineTo(x + o + s, y + o + s * 0.94);
				tri.lineTo(x + o, y + o + s * 0.94);
				tri.closePath();
				g.setColor(Theme.alpha(c, 60));
				g.fill(tri);
				g.setColor(c);
				g.draw(tri);
				g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.drawLine(Math.round(x + o + s / 2), Math.round(y + o + s * 0.36f), Math.round(x + o + s / 2), Math.round(y + o + s * 0.6f));
				g.fill(new Ellipse2D.Float(x + o + s / 2 - 1.1f, y + o + s * 0.72f, 2.2f, 2.2f));
				break;
			}
			case BAD:
			{
				g.setColor(Theme.alpha(c, 70));
				g.fill(circle);
				g.setColor(c);
				g.draw(circle);
				float a = s * 0.32f;
				float m = s / 2 + o;
				g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.drawLine(Math.round(x + m - a), Math.round(y + m - a), Math.round(x + m + a), Math.round(y + m + a));
				g.drawLine(Math.round(x + m + a), Math.round(y + m - a), Math.round(x + m - a), Math.round(y + m + a));
				break;
			}
			case NOTE:
			{
				g.setColor(Theme.alpha(c, 40));
				g.fill(circle);
				g.setColor(Theme.alpha(c, 170));
				g.draw(circle);
				float dot = s * 0.34f;
				g.setColor(c);
				g.fill(new Ellipse2D.Float(x + o + (s - dot) / 2, y + o + (s - dot) / 2, dot, dot));
				break;
			}
			default:
			{
				g.setColor(c);
				g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{2f, 2f}, 0));
				g.draw(circle);
			}
		}
	}
}
