package com.codex.gearroadmap.overlay;

import com.codex.gearroadmap.ui.Sigil;
import com.codex.gearroadmap.ui.Theme;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A gilt plaque that rises in the game view when the player gets an upgrade from their
 * road: the item, the damage it adds against the aimed target and what comes next. It
 * draws a view the plugin prepares, so rendering never touches game state.
 */
public class UpgradePlaque extends Overlay
{
	private static final int WIDTH = 228;
	private static final int PAD = 9;
	private static final int ICON = 40;
	private static final long FADE_MS = 450;

	/** What the plaque shows. */
	public static final class View
	{
		final String heading;
		final String name;
		final String gain;
		final String next;
		final Color color;
		final BufferedImage icon;
		final long shownAt;
		final long durationMs;

		public View(String heading, String name, String gain, String next, Color color, BufferedImage icon,
			long shownAt, long durationMs)
		{
			this.heading = heading;
			this.name = name;
			this.gain = gain;
			this.next = next;
			this.color = color;
			this.icon = icon;
			this.shownAt = shownAt;
			this.durationMs = durationMs;
		}
	}

	private volatile View view;

	public UpgradePlaque()
	{
		setPosition(OverlayPosition.TOP_CENTER);
	}

	public void show(View view)
	{
		this.view = view;
	}

	public void clear()
	{
		view = null;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		View v = view;
		if (v == null)
		{
			return null;
		}
		long age = System.currentTimeMillis() - v.shownAt;
		if (age > v.durationMs)
		{
			view = null;
			return null;
		}
		float alpha = (float) Math.min(1, Math.min(age / (double) FADE_MS, (v.durationMs - age) / (double) FADE_MS));
		Composite old = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alpha)));
		Theme.smooth(g);

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		int lines = 2 + (v.gain == null ? 0 : 1) + (v.next == null ? 0 : 1);
		int textHeight = small.getHeight() + bold.getHeight() + (lines - 2) * (small.getHeight() + 1);
		int height = PAD * 2 + Math.max(ICON, textHeight);
		Color tint = v.color;

		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, WIDTH - 1, height - 1, 12, 12);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 238), 0, height, Theme.alpha(Theme.INK_DEEP, 238)));
		g.fill(shape);
		double beat = 0.5 + 0.5 * Math.sin(age / 220.0);
		g.setStroke(new BasicStroke(3f));
		g.setColor(Theme.alpha(tint, (int) (40 + beat * 70)));
		g.draw(shape);
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(tint, 200));
		g.draw(shape);

		// The item sits in a lit well.
		int wellX = PAD;
		int wellY = (height - ICON) / 2;
		g.setPaint(new RadialGradientPaint(wellX + ICON / 2f, wellY + ICON / 2f, ICON / 2f,
			new float[]{0f, 1f}, new Color[]{Theme.alpha(tint, 110), Theme.alpha(Theme.INK_DEEP, 220)}));
		g.fill(new Ellipse2D.Float(wellX, wellY, ICON, ICON));
		g.setColor(Theme.alpha(tint, 170));
		g.draw(new Ellipse2D.Float(wellX, wellY, ICON, ICON));
		if (v.icon != null)
		{
			g.drawImage(v.icon, wellX + (ICON - v.icon.getWidth()) / 2, wellY + (ICON - v.icon.getHeight()) / 2, null);
		}
		else
		{
			Sigil.paint(g, wellX + 8, wellY + 8, ICON - 16);
		}

		int x = PAD + ICON + 9;
		int width = WIDTH - x - PAD;
		int y = PAD + Math.max(0, (Math.max(ICON, textHeight) - textHeight) / 2);
		g.setFont(Theme.small());
		shadow(g, v.heading, x, y + small.getAscent(), tint);
		y += small.getHeight();
		g.setFont(Theme.title());
		shadow(g, clip(v.name, bold, width), x, y + bold.getAscent(), Theme.GOLD_BRIGHT);
		y += bold.getHeight();
		g.setFont(Theme.small());
		if (v.gain != null)
		{
			shadow(g, clip(v.gain, small, width), x, y + small.getAscent(), Theme.TEAL);
			y += small.getHeight() + 1;
		}
		if (v.next != null)
		{
			shadow(g, clip(v.next, small, width), x, y + small.getAscent(), Theme.TEXT_MUTED);
		}
		g.setComposite(old);
		return new Dimension(WIDTH, height);
	}

	private static void shadow(Graphics2D g, String text, int x, int y, Color color)
	{
		g.setColor(Color.BLACK);
		g.drawString(text, x + 1, y + 1);
		g.setColor(color);
		g.drawString(text, x, y);
	}

	private static String clip(String text, FontMetrics fm, int width)
	{
		if (text == null)
		{
			return "";
		}
		if (fm.stringWidth(text) <= width)
		{
			return text;
		}
		String ellipsis = "...";
		int end = text.length();
		while (end > 0 && fm.stringWidth(text.substring(0, end) + ellipsis) > width)
		{
			end--;
		}
		return text.substring(0, end) + ellipsis;
	}
}
