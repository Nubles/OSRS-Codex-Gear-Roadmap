package com.codex.gearroadmap.ui;

import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** A slim rounded bar that fills with the same ember-to-teal ramp as the rings. */
public class ProgressBar extends JComponent
{
	private final Animator animator = new Animator(this);
	private boolean complete;

	public ProgressBar(int height)
	{
		setPreferredSize(new Dimension(10, height));
		setMinimumSize(new Dimension(10, height));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		setOpaque(false);
	}

	public void setProgress(double progress, boolean complete, boolean animate)
	{
		this.complete = complete;
		animator.set(Math.max(0, Math.min(1, progress)), animate);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		float arc = h;
		g.setColor(Theme.INK_DEEP);
		g.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
		double value = animator.get();
		if (value > 0)
		{
			float fill = (float) Math.max(h, w * value);
			if (complete)
			{
				g.setColor(Theme.GREEN);
			}
			else
			{
				g.setPaint(new GradientPaint(0, 0, Theme.progressColor(0), w, 0, Theme.progressColor(1)));
			}
			g.fill(new RoundRectangle2D.Float(0, 0, fill, h, arc, arc));
			g.setColor(Theme.alpha(java.awt.Color.WHITE, 40));
			g.drawLine(Math.round(arc / 2), 0, Math.round(fill - arc / 2), 0);
		}
		g.setColor(Theme.alpha(Theme.STROKE, 160));
		g.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc));
		g.dispose();
	}
}
