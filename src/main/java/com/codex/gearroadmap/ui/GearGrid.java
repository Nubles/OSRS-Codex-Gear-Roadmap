package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Loadout;
import com.codex.gearroadmap.data.Gear;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;

/**
 * The loadout laid out like the game's worn equipment screen. Pieces new to the player
 * glow teal; empty slots show a faint engraving of what belongs there.
 */
public class GearGrid extends JComponent
{
	private static final int TILE_W = 38;
	private static final int TILE_H = 34;
	private static final int GAP = 6;
	/** Column and row of each slot, in the order of Gear's slot numbers. */
	private static final int[][] CELLS = {
		{1, 0}, {0, 1}, {1, 1}, {2, 1}, {0, 2}, {1, 2}, {2, 2}, {1, 3}, {0, 4}, {1, 4}, {2, 4},
	};
	private static final String[] ENGRAVING = {"Hd", "Cp", "Nk", "Am", "Wp", "Bd", "Sh", "Lg", "Gl", "Bt", "Rg"};

	private final Loadout loadout;
	private final Set<Gear> fresh;
	private final Color accent;
	private final Map<Integer, BufferedImage> images = new HashMap<>();
	private int hover = -1;

	public GearGrid(Loadout loadout, Set<Gear> fresh, Color accent, ItemIcons icons)
	{
		this.loadout = loadout;
		this.fresh = fresh == null ? Collections.emptySet() : fresh;
		this.accent = accent;
		setPreferredSize(new Dimension(10, 5 * TILE_H + 4 * GAP + 4));
		setOpaque(false);
		for (int slot = 0; slot < Gear.SLOTS; slot++)
		{
			Gear g = loadout.get(slot);
			if (g != null && icons != null)
			{
				images.put(slot, icons.icon(g.getId(), this::repaint));
			}
		}
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				int slot = slotAt(e.getX(), e.getY());
				if (slot != hover)
				{
					hover = slot;
					Gear g = slot < 0 ? null : loadout.get(slot);
					setToolTipText(slot < 0 ? null : g == null ? Gear.SLOT_NAMES[slot] + ": empty" : g.getName());
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = -1;
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	private int left()
	{
		return (getWidth() - (3 * TILE_W + 2 * GAP)) / 2;
	}

	private int slotAt(int x, int y)
	{
		for (int slot = 0; slot < Gear.SLOTS; slot++)
		{
			int tx = left() + CELLS[slot][0] * (TILE_W + GAP);
			int ty = 2 + CELLS[slot][1] * (TILE_H + GAP);
			if (x >= tx && x < tx + TILE_W && y >= ty && y < ty + TILE_H)
			{
				return slot;
			}
		}
		return -1;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int left = left();

		// Faint gilt threads joining the slots, like the game's equipment frame.
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 70));
		int cx = left + TILE_W + GAP + TILE_W / 2;
		g.drawLine(cx, 2 + TILE_H, cx, 2 + 4 * (TILE_H + GAP));
		int midY = 2 + 2 * (TILE_H + GAP) + TILE_H / 2;
		g.drawLine(left + TILE_W, midY, left + 2 * (TILE_W + GAP), midY);
		int capeY = 2 + (TILE_H + GAP) + TILE_H / 2;
		g.drawLine(left + TILE_W, capeY, left + 2 * (TILE_W + GAP), capeY);

		for (int slot = 0; slot < Gear.SLOTS; slot++)
		{
			int tx = left + CELLS[slot][0] * (TILE_W + GAP);
			int ty = 2 + CELLS[slot][1] * (TILE_H + GAP);
			drawTile(g, slot, tx, ty);
		}
		g.dispose();
	}

	private void drawTile(Graphics2D g, int slot, int x, int y)
	{
		Gear gear = loadout.get(slot);
		boolean isFresh = gear != null && fresh.contains(gear);
		RoundRectangle2D shape = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, TILE_W - 1, TILE_H - 1, 8, 8);
		Color top = gear == null ? Theme.INK_DEEP : Theme.mix(Theme.CARD_TOP, isFresh ? Theme.TEAL : accent, 0.16);
		g.setPaint(new GradientPaint(x, y, top, x, y + TILE_H, gear == null ? Theme.INK_DEEP : Theme.CARD));
		g.fill(shape);
		Color rim = isFresh ? Theme.TEAL : gear == null ? Theme.STROKE_SOFT : Theme.alpha(Theme.GOLD, 150);
		if (hover == slot)
		{
			rim = Theme.GOLD_BRIGHT;
		}
		g.setStroke(new BasicStroke(isFresh ? 1.4f : 1f));
		g.setColor(rim);
		g.draw(shape);
		if (gear == null)
		{
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			String text = ENGRAVING[slot];
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(text, x + (TILE_W - fm.stringWidth(text)) / 2, y + (TILE_H + fm.getAscent()) / 2 - 1);
			return;
		}
		BufferedImage image = images.get(slot);
		if (image != null)
		{
			Composite old = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			g.drawImage(image, x + (TILE_W - image.getWidth()) / 2, y + (TILE_H - image.getHeight()) / 2, null);
			g.setComposite(old);
		}
		g.setColor(Theme.alpha(Color.WHITE, 16));
		g.drawLine(x + 5, y + 1, x + TILE_W - 5, y + 1);
	}
}
