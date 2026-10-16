package com.codex.gearroadmap.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/** A small-caps heading with a gold diamond and a line that fades into the dark. */
public class SectionHeader extends JComponent
{
	private final String text;
	private final String trailing;

	public SectionHeader(String text)
	{
		this(text, null);
	}

	public SectionHeader(String text, String trailing)
	{
		this.text = text.toUpperCase();
		this.trailing = trailing;
		setFont(Theme.small());
		setPreferredSize(new Dimension(10, 20));
		setMinimumSize(new Dimension(10, 20));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		setAlignmentX(LEFT_ALIGNMENT);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int h = getHeight();
		int mid = h / 2 + 2;
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(4, mid - 4);
		diamond.lineTo(8, mid);
		diamond.lineTo(4, mid + 4);
		diamond.lineTo(0, mid);
		diamond.closePath();
		g.setColor(Theme.GOLD);
		g.fill(diamond);

		g.setFont(getFont());
		FontMetrics fm = g.getFontMetrics();
		int textX = 13;
		int baseline = mid + fm.getAscent() / 2 - 1;
		g.setColor(Theme.GOLD);
		g.drawString(text, textX, baseline);

		int lineStart = textX + fm.stringWidth(text) + 6;
		int lineEnd = getWidth();
		if (trailing != null)
		{
			int tw = fm.stringWidth(trailing);
			lineEnd = getWidth() - tw - 6;
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(trailing, getWidth() - tw, baseline);
		}
		if (lineEnd > lineStart)
		{
			g.setPaint(new GradientPaint(lineStart, 0, Theme.alpha(Theme.GOLD, 110), lineEnd, 0, Theme.alpha(Theme.GOLD, 0)));
			g.drawLine(lineStart, mid, lineEnd, mid);
		}
		g.dispose();
	}
}
