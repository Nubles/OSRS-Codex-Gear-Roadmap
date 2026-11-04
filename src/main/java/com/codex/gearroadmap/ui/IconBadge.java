package com.codex.gearroadmap.ui;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/** An item picture sitting in a small dark well, used at the start of list rows. */
public class IconBadge extends JComponent
{
	private final boolean owned;
	private BufferedImage image;
	private boolean sea;

	public IconBadge(int itemId, ItemIcons icons, boolean owned)
	{
		this.owned = owned;
		Dimension d = new Dimension(28, 26);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
		setOpaque(false);
		if (icons != null && itemId > 0)
		{
			image = icons.icon(itemId, this::repaint);
		}
	}

	/** Seals the badge with a small wave: the item comes from Sailing's waters. */
	public IconBadge sailing(boolean fromSailing)
	{
		sea = fromSailing;
		repaint();
		return this;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
		g.fill(new Ellipse2D.Float(1, 1, w - 2, h - 2));
		g.setColor(Theme.alpha(owned ? Theme.GOLD : Theme.STROKE, owned ? 120 : 200));
		g.draw(new Ellipse2D.Float(1, 1, w - 2, h - 2));
		if (image != null)
		{
			int iw = Math.min(image.getWidth(), 36);
			int ih = Math.min(image.getHeight(), 32);
			double scale = Math.min((w - 2) / (double) iw, (h - 2) / (double) ih);
			int dw = (int) Math.round(iw * scale);
			int dh = (int) Math.round(ih * scale);
			if (!owned)
			{
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
			}
			g.drawImage(image, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
		}
		if (sea)
		{
			paintWave(g, w - 11, h - 11, 10);
		}
		g.dispose();
	}

	/** A teal seal with a crest of water in it, drawn at the given corner. */
	static void paintWave(Graphics2D g, float x, float y, float size)
	{
		Graphics2D w = (Graphics2D) g.create();
		w.setComposite(AlphaComposite.SrcOver);
		Theme.smooth(w);
		w.setColor(Theme.INK_DEEP);
		w.fill(new Ellipse2D.Float(x, y, size, size));
		w.setColor(Theme.alpha(Theme.TEAL, 200));
		w.setStroke(new BasicStroke(1f));
		w.draw(new Ellipse2D.Float(x, y, size, size));
		Path2D.Float crest = new Path2D.Float();
		float mid = y + size * 0.58f;
		float step = size / 4f;
		crest.moveTo(x + size * 0.16f, mid);
		crest.quadTo(x + size * 0.16f + step * 0.5f, mid - step * 0.9f, x + size * 0.16f + step, mid);
		crest.quadTo(x + size * 0.16f + step * 1.5f, mid + step * 0.9f, x + size * 0.16f + step * 2, mid);
		crest.quadTo(x + size * 0.16f + step * 2.5f, mid - step * 0.9f, x + size * 0.84f, mid);
		w.setColor(Theme.TEAL);
		w.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		w.draw(crest);
		w.dispose();
	}
}
