package com.codex.gearroadmap.ui;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/** An item picture sitting in a small dark well, used at the start of list rows. */
public class IconBadge extends JComponent
{
	private final boolean owned;
	private BufferedImage image;

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
		g.dispose();
	}
}
