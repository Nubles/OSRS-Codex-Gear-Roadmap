package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Result;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import net.runelite.client.ui.FontManager;

/**
 * The Codex look: warm ink, aged gold and a single arcane teal, drawn with the
 * game's own fonts so the panel reads as part of Gielinor rather than a web page.
 * Gear Roadmap gives each kind of attack its own colour: ember for melee, green for
 * ranged and violet for magic.
 */
public final class Theme
{
	public static final Color INK = new Color(23, 20, 16);
	public static final Color INK_DEEP = new Color(16, 14, 11);
	public static final Color CARD = new Color(34, 30, 24);
	public static final Color CARD_TOP = new Color(42, 37, 29);
	public static final Color CARD_HOVER = new Color(48, 42, 33);
	public static final Color STROKE = new Color(66, 56, 42);
	public static final Color STROKE_SOFT = new Color(48, 41, 32);

	public static final Color GOLD = new Color(214, 178, 106);
	public static final Color GOLD_BRIGHT = new Color(246, 214, 144);
	public static final Color GOLD_DIM = new Color(132, 108, 66);
	public static final Color TEAL = new Color(96, 184, 170);
	public static final Color GREEN = new Color(130, 196, 108);
	public static final Color EMBER = new Color(222, 134, 86);
	public static final Color BLOCKED = new Color(176, 92, 84);
	public static final Color BLOOD = new Color(204, 70, 60);
	public static final Color BLOOD_DEEP = new Color(120, 32, 28);
	public static final Color VIOLET = new Color(176, 132, 222);

	public static final Color TEXT = new Color(234, 224, 202);
	public static final Color TEXT_MUTED = new Color(160, 148, 126);
	public static final Color TEXT_FAINT = new Color(110, 100, 84);

	public static final int RADIUS = 10;
	public static final int GUTTER = 8;

	private Theme()
	{
	}

	public static Font title()
	{
		return FontManager.getRunescapeBoldFont();
	}

	public static Font body()
	{
		return FontManager.getRunescapeFont();
	}

	public static Font small()
	{
		return FontManager.getRunescapeSmallFont();
	}

	public static Color alpha(Color c, int alpha)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	public static Color mix(Color a, Color b, double t)
	{
		t = Math.max(0, Math.min(1, t));
		return new Color(
			(int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
			(int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
			(int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
			(int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
	}

	/** Progress colour: ember when just begun, gold through the middle, green when nearly there. */
	public static Color progressColor(double progress)
	{
		if (progress >= 1)
		{
			return GREEN;
		}
		if (progress < 0.5)
		{
			return mix(EMBER, GOLD, progress * 2);
		}
		return mix(GOLD, TEAL, (progress - 0.5) * 2);
	}

	/** The colour of a kind of attack. */
	public static Color kind(Result.Kind kind)
	{
		if (kind == null)
		{
			return TEXT_MUTED;
		}
		switch (kind)
		{
			case MELEE:
				return EMBER;
			case RANGED:
				return GREEN;
			default:
				return VIOLET;
		}
	}

	/** Smooth shapes, but the pixel fonts stay crisp. */
	public static void smooth(Graphics2D g)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}
}
