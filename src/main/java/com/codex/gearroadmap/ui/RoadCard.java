package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Fights;
import com.codex.gearroadmap.core.Loadout;
import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Result;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.core.Savings;
import com.codex.gearroadmap.data.Gear;
import com.codex.gearroadmap.data.Target;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The road as a picture to share: the player's loadout today, the damage it does and what the
 * road ahead would make of it, each stop with its item and price on a winding gilt road, the
 * savings goal and how long it should take with Codex Ledger, and the buy that would save the
 * most time where Codex Session Chronicle says the player fights. Drawn at twice the panel's
 * scale on the suite's gilt page so the game's pixel fonts stay crisp.
 */
public final class RoadCard
{
	static final int WIDTH = 360;
	static final int SCALE = 2;
	/** Stops drawn; four to a row. */
	static final int STOPS = 8;
	private static final int PER_ROW = 4;
	private static final int PAD = 20;
	private static final int STOP_H = 90;
	private static final String[] ENGRAVING = {"Hd", "Cp", "Nk", "Am", "Wp", "Bd", "Sh", "Lg", "Gl", "Bt", "Rg"};
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

	private RoadCard()
	{
	}

	/** True when there is a road to draw: logged in with a plan made. */
	public static boolean canDraw(Report report)
	{
		return report != null && report.isLoggedIn() && report.getRoadmap() != null;
	}

	/** "Gilded Owl's road to Vorkath", or "My road to Vorkath" without the name. */
	public static String title(String player, Target target)
	{
		String who = player == null || player.isEmpty() ? "My road" : player + "'s road";
		return who + " to " + target.getName();
	}

	/** What the whole road does: "+40% DPS for 156M", "Best in slot here" when nothing is left to buy. */
	public static String verdict(Roadmap map, boolean ironman)
	{
		if (map.getPath().isEmpty())
		{
			return "Best in slot for this fight";
		}
		long total = map.getPath().get(map.getPath().size() - 1).getSpent();
		double now = map.getNow().dps();
		long share = now <= 0 ? 0 : Math.round((map.finalDps() / now - 1) * 100);
		return "+" + share + "% damage " + (ironman ? "for gear worth " : "for ") + Format.coins(total);
	}

	/** Renders the card; pictures still loading are left out and onLoaded runs when one arrives. */
	public static BufferedImage render(Report report, String player, ItemIcons icons, long now, Runnable onLoaded)
	{
		BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = scratch.createGraphics();
		int height = content(sg, report, player, icons, now, onLoaded);
		sg.dispose();

		BufferedImage image = new BufferedImage(WIDTH * SCALE, height * SCALE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.scale(SCALE, SCALE);
		Theme.smooth(g);
		frame(g, height);
		content(g, report, player, icons, now, onLoaded);
		g.dispose();
		return image;
	}

	private static void frame(Graphics2D g, int h)
	{
		int w = WIDTH;
		RoundRectangle2D page = new RoundRectangle2D.Float(0, 0, w, h, 18, 18);
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK, Theme.CARD_TOP, 0.5), 0, h, Theme.INK_DEEP));
		g.fill(page);
		g.setClip(page);
		g.setPaint(new RadialGradientPaint(w / 2f, 40, w * 0.8f, new float[]{0f, 1f},
			new Color[]{Theme.alpha(Theme.GOLD, 46), Theme.alpha(Theme.GOLD, 0)}));
		g.fillRect(0, 0, w, h);
		// The same faint grain every time, so the page reads as vellum.
		Random grain = new Random(11);
		for (int i = 0; i < w * h / 26; i++)
		{
			g.setColor(Theme.alpha(grain.nextBoolean() ? Theme.GOLD_BRIGHT : Color.BLACK, 6 + grain.nextInt(8)));
			g.fillRect(grain.nextInt(w), grain.nextInt(h), 1, 1);
		}
		g.setClip(null);
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 190));
		g.draw(new RoundRectangle2D.Float(4.5f, 4.5f, w - 9, h - 9, 14, 14));
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 120));
		g.draw(new RoundRectangle2D.Float(8.5f, 8.5f, w - 17, h - 17, 10, 10));
		float[][] corners = {{14, 14}, {w - 14, 14}, {14, h - 14}, {w - 14, h - 14}};
		for (float[] c : corners)
		{
			diamond(g, c[0], c[1], 3.5f, Theme.GOLD);
		}
	}

	private static int content(Graphics2D g, Report report, String player, ItemIcons icons, long now, Runnable onLoaded)
	{
		Theme.smooth(g);
		Roadmap map = report.getRoadmap();
		Target target = map.getTarget();
		int x = PAD;
		int cw = WIDTH - PAD * 2;
		int y = PAD + 4;

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		Sigil.paint(g, x, y - 2, 13);
		g.setColor(Theme.GOLD);
		g.drawString("GEAR ROADMAP", x + 18, y + small.getAscent() - 1);
		String date = DATE.format(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()));
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(date, x + cw - small.stringWidth(date), y + small.getAscent() - 1);
		y += small.getHeight() + 8;

		// The title as large as fits: the name and the target are what a friend reads first.
		// A long name and target take two lines at the middle size rather than shrink to body text.
		String title = title(player, target);
		Font large = Theme.title().deriveFont(Theme.title().getSize2D() * 2f);
		String[] lines = {title};
		if (g.getFontMetrics(large).stringWidth(title) > cw)
		{
			large = Theme.title().deriveFont(Theme.title().getSize2D() * 1.5f);
			int to = title.lastIndexOf(" to ");
			if (g.getFontMetrics(large).stringWidth(title) > cw && to > 0)
			{
				lines = new String[]{title.substring(0, to), title.substring(to + 1)};
			}
		}
		g.setFont(large);
		FontMetrics lm = g.getFontMetrics();
		for (String line : lines)
		{
			ProgressRing.drawShadowed(g, clip(line, lm, cw), x, y + lm.getAscent() - 4, Theme.GOLD_BRIGHT);
			y += lm.getAscent() - 2;
		}
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		ProgressRing.drawShadowed(g, clip(verdict(map, report.isIronman()), bold, cw), x, y + bold.getAscent(), Theme.TEXT);
		y += bold.getHeight();
		Result nowResult = map.getNow().best().getResult();
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_MUTED);
		String style = nowResult.getStyle() == null ? "" : ", " + nowResult.getStyle().toLowerCase(Locale.ROOT);
		String boosts = report.getBoosts() == null ? "" : ", " + report.getBoosts().toLowerCase(Locale.ROOT);
		g.drawString(clip(kindName(nowResult.getKind()) + style + boosts, small, cw), x, y + small.getAscent());
		y += small.getHeight() + 8;
		rule(g, x, y, cw);
		y += 12;

		y = dpsBand(g, map, target, x, y, cw) + 12;

		header(g, "Today's loadout", Format.dps(nowResult.getDps()) + " DPS", x, y, cw);
		y += 24;
		y = loadout(g, map.getNow().best().getLoadout(), icons, onLoaded, x, y, cw) + 12;

		List<Roadmap.Step> path = map.getPath();
		if (!path.isEmpty())
		{
			int shown = Math.min(STOPS, path.size());
			String trailing = path.size() > shown ? shown + " of " + path.size() + " stops" : path.size() + (path.size() == 1 ? " stop" : " stops");
			header(g, "The road ahead", trailing, x, y, cw);
			y += 24;
			y = road(g, path.subList(0, shown), icons, onLoaded, x, y, cw) + 8;
		}

		Savings.View savings = report.getSavings();
		if (savings != null)
		{
			y = savings(g, savings, x, y, cw) + 12;
		}

		List<Fights.Pick> fights = report.getFights();
		Fights.Tally tally = report.getTally();
		if (fights != null && !fights.isEmpty() && tally != null && !tally.isEmpty())
		{
			Fights.Pick pick = fights.get(0);
			int days = Math.max(1, tally.getDays());
			header(g, "Where I fight", "last " + days + (days == 1 ? " day" : " days"), x, y, cw);
			y += 24;
			BufferedImage icon = icons == null || pick.getIcon() == null ? null : icons.icon(pick.getIcon().getId(), onLoaded);
			int tx = x;
			if (icon != null)
			{
				g.drawImage(icon, x, y - 4, null);
				tx = x + icon.getWidth() + 8;
			}
			g.setFont(Theme.body());
			FontMetrics fm = g.getFontMetrics();
			ProgressRing.drawShadowed(g, clip(pick.getName() + " would have saved " + Format.span(pick.getSeconds()), fm, x + cw - tx), tx,
				y + fm.getAscent() - 2, Theme.TEXT);
			g.setFont(Theme.small());
			g.setColor(Theme.TEXT_MUTED);
			List<String> at = pick.getWhere();
			String where = at.size() <= 1 ? String.join("", at)
				: String.join(", ", at.subList(0, at.size() - 1)) + " and " + at.get(at.size() - 1);
			g.drawString(clip(where.isEmpty() ? "over the fights I log most" : "at " + where + ", the fights I log most", small, x + cw - tx), tx,
				y + fm.getAscent() + small.getAscent());
			y += 34;
		}

		y += 2;
		rule(g, x, y, cw);
		y += 8;
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_FAINT);
		g.drawString("Codex Gear Roadmap for RuneLite", x, y + small.getAscent());
		String coins = report.isIronman() ? "Ironman" : map.getCoins() <= 0 ? "No coins yet" : Format.coins(map.getCoins()) + " in coins";
		g.drawString(coins, x + cw - small.stringWidth(coins), y + small.getAscent());
		y += small.getHeight();
		return y + PAD;
	}

	/** Damage now and at the road's end, the kill time each way and the max hit. */
	private static int dpsBand(Graphics2D g, Roadmap map, Target target, int x, int y, int cw)
	{
		Result now = map.getNow().best().getResult();
		List<Roadmap.Step> path = map.getPath();
		Result end = path.isEmpty() ? now : path.get(path.size() - 1).getOption().getAfter().getResult();
		int h = 62;
		RoundRectangle2D box = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, cw - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(x, y, Theme.CARD_TOP, x, y + h, Theme.CARD));
		g.fill(box);
		g.setColor(Theme.alpha(Theme.GOLD, 120));
		g.draw(box);

		int half = cw / 2;
		column(g, "NOW", Format.dps(now.getDps()), killLine(now, target), Theme.TEXT, x + 12, y);
		if (path.isEmpty())
		{
			// Nothing left to buy: the right half says so rather than repeat the same figures.
			g.setFont(Theme.body());
			FontMetrics fm = g.getFontMetrics();
			ProgressRing.drawShadowed(g, "Nothing left", x + half + 18, y + 26, Theme.GOLD_BRIGHT);
			ProgressRing.drawShadowed(g, "to buy here", x + half + 18, y + 26 + fm.getHeight(), Theme.GOLD_BRIGHT);
			return y + h;
		}
		String endLabel = end.getKind() != now.getKind() ? "ROAD'S END, " + kindName(end.getKind()).toUpperCase(Locale.ROOT) : "ROAD'S END";
		column(g, endLabel, Format.dps(end.getDps()), killLine(end, target), Theme.GOLD_BRIGHT, x + half + 18, y);
		// An arrow between the two, gilt.
		float ax = x + half - 2;
		float ay = y + 30;
		g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.GOLD);
		g.draw(new java.awt.geom.Line2D.Float(ax - 12, ay, ax + 8, ay));
		Path2D head = new Path2D.Float();
		head.moveTo(ax + 3, ay - 5);
		head.lineTo(ax + 9, ay);
		head.lineTo(ax + 3, ay + 5);
		g.draw(head);
		return y + h;
	}

	private static String killLine(Result r, Target target)
	{
		double seconds = r.secondsToKill(target);
		String kill = Double.isInfinite(seconds) || Double.isNaN(seconds) ? "no kill" : Format.time(seconds) + " a kill";
		return kill + ", max hit " + r.getMaxHit();
	}

	private static void column(Graphics2D g, String label, String dps, String detail, Color color, int x, int y)
	{
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(label, x, y + 8 + small.getAscent());
		Font big = Theme.title().deriveFont(Theme.title().getSize2D() * 1.5f);
		g.setFont(big);
		FontMetrics bm = g.getFontMetrics();
		int base = y + 10 + small.getHeight() + bm.getAscent() - 6;
		ProgressRing.drawShadowed(g, dps, x, base, color);
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(" DPS", x + bm.stringWidth(dps), base);
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(clip(detail, small, 140), x, base + small.getHeight());
	}

	/** The eleven worn slots in a row, as the equipment screen keeps them. */
	private static int loadout(Graphics2D g, Loadout loadout, ItemIcons icons, Runnable onLoaded, int x, int y, int cw)
	{
		int gap = 3;
		int tile = (cw - gap * (Gear.SLOTS - 1)) / Gear.SLOTS;
		int th = tile + 4;
		for (int slot = 0; slot < Gear.SLOTS; slot++)
		{
			int tx = x + slot * (tile + gap);
			RoundRectangle2D cell = new RoundRectangle2D.Float(tx + 0.5f, y + 0.5f, tile - 1, th - 1, 6, 6);
			g.setColor(Theme.INK_DEEP);
			g.fill(cell);
			Gear piece = loadout.get(slot);
			g.setColor(piece == null ? Theme.STROKE_SOFT : Theme.alpha(Theme.GOLD_DIM, 160));
			g.draw(cell);
			if (piece == null)
			{
				// An empty slot is engraved with its name, as the equipment screen leaves its outline.
				g.setFont(Theme.small());
				FontMetrics fm = g.getFontMetrics();
				String mark = ENGRAVING[slot];
				g.setColor(Theme.alpha(Theme.TEXT_FAINT, 150));
				g.drawString(mark, tx + (tile - fm.stringWidth(mark)) / 2, y + th / 2 + fm.getAscent() / 2 - 1);
			}
			BufferedImage icon = piece == null || icons == null ? null : icons.icon(piece.getId(), onLoaded);
			if (icon != null)
			{
				fit(g, icon, tx + 1, y + 2, tile - 2, th - 4, 1f);
			}
		}
		return y + th;
	}

	/** The stops, four to a row on a serpentine road; the stretch the coins cover glows gold. */
	private static int road(Graphics2D g, List<Roadmap.Step> stops, ItemIcons icons, Runnable onLoaded, int x, int y, int cw)
	{
		int gap = 8;
		int colW = (cw - gap * (PER_ROW - 1)) / PER_ROW;
		int rows = (stops.size() + PER_ROW - 1) / PER_ROW;
		float[] cx = new float[stops.size()];
		float[] cy = new float[stops.size()];
		for (int i = 0; i < stops.size(); i++)
		{
			int row = i / PER_ROW;
			int col = row % 2 == 0 ? i % PER_ROW : PER_ROW - 1 - i % PER_ROW;
			cx[i] = x + col * (colW + gap) + colW / 2f;
			cy[i] = y + row * (STOP_H + 6) + 20;
		}
		// The road first, behind the stops: solid gold as far as the coins go, dashed beyond.
		for (int i = 1; i < stops.size(); i++)
		{
			Path2D leg = new Path2D.Float();
			leg.moveTo(cx[i - 1], cy[i - 1]);
			if (cy[i] != cy[i - 1])
			{
				// The turn at a row's end bends round the outside like a road on a hill, clear of the names.
				float side = cx[i] > x + cw / 2f ? x + cw + 16 : x - 16;
				leg.curveTo(side, cy[i - 1], side, cy[i], cx[i], cy[i]);
			}
			else
			{
				leg.lineTo(cx[i], cy[i]);
			}
			boolean lit = stops.get(i).isAffordable();
			g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(Theme.alpha(Theme.INK_DEEP, 220));
			g.draw(leg);
			g.setStroke(lit ? new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
				: new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0));
			g.setColor(lit ? Theme.GOLD : Theme.alpha(Theme.GOLD_DIM, 170));
			g.draw(leg);
		}
		for (int i = 0; i < stops.size(); i++)
		{
			stop(g, stops.get(i), i + 1, icons, onLoaded, cx[i], cy[i], colW);
		}
		return y + rows * (STOP_H + 6) - 6;
	}

	/** One stop: its item in a ring with the step number, then its name, price and the damage it adds. */
	private static void stop(Graphics2D g, Roadmap.Step step, int number, ItemIcons icons, Runnable onLoaded, float cx, float cy, int colW)
	{
		boolean lit = step.isAffordable();
		Roadmap.Option o = step.getOption();
		float r = 15;
		if (lit)
		{
			g.setColor(Theme.alpha(Theme.GOLD, 40));
			g.fill(new Ellipse2D.Float(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2));
		}
		g.setColor(Theme.INK_DEEP);
		g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		g.setStroke(new BasicStroke(1.4f));
		g.setColor(lit ? Theme.GOLD : Theme.STROKE);
		g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		BufferedImage icon = icons == null ? null : icons.icon(o.getIcon().getId(), onLoaded);
		if (icon != null)
		{
			fit(g, icon, Math.round(cx - r + 3), Math.round(cy - r + 3), Math.round(r * 2 - 6), Math.round(r * 2 - 6), lit ? 1f : 0.65f);
		}
		// A pair's second piece sits in a small well on the other shoulder.
		Gear partner = partner(o);
		if (partner != null)
		{
			float px = cx - r + 3;
			float py = cy + r - 5;
			g.setColor(Theme.INK_DEEP);
			g.fill(new Ellipse2D.Float(px - 8, py - 8, 16, 16));
			g.setColor(lit ? Theme.GOLD : Theme.TEXT_FAINT);
			g.draw(new Ellipse2D.Float(px - 8, py - 8, 16, 16));
			BufferedImage second = icons == null ? null : icons.icon(partner.getId(), onLoaded);
			if (second != null)
			{
				fit(g, second, Math.round(px - 6), Math.round(py - 6), 12, 12, lit ? 1f : 0.65f);
			}
		}
		// The number on the ring's shoulder.
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String label = String.valueOf(number);
		float bx = cx + r - 3;
		float by = cy - r + 2;
		g.setColor(Theme.INK_DEEP);
		g.fill(new Ellipse2D.Float(bx - 6, by - 6, 12, 12));
		g.setColor(lit ? Theme.GOLD : Theme.TEXT_FAINT);
		g.draw(new Ellipse2D.Float(bx - 6, by - 6, 12, 12));
		ProgressRing.drawShadowed(g, label, Math.round(bx - small.stringWidth(label) / 2f), Math.round(by + small.getAscent() / 2f - 1),
			lit ? Theme.GOLD_BRIGHT : Theme.TEXT_MUTED);

		int left = Math.round(cx - colW / 2f);
		int ty = Math.round(cy + r + 4);
		g.setFont(Theme.small());
		// The name may take two lines, so "Dragon hunter lance" is read whole under its ring.
		// A one-line name sits on the second line, just over its price, so prices across a row stay level.
		List<String> name = nameLines(o.getName(), small, colW);
		int line = 2 - name.size();
		for (String text : name)
		{
			centred(g, text, left, colW, ty + small.getAscent() + small.getHeight() * line++, lit ? Theme.TEXT : Theme.TEXT_MUTED);
		}
		line = 2;
		String price = priceLine(o, small, colW);
		centred(g, price, left, colW, ty + small.getAscent() + small.getHeight() * line, lit ? Theme.TEAL : Theme.GOLD);
		centred(g, Format.gain(o.getGain()) + " DPS", left, colW, ty + small.getAscent() + small.getHeight() * (line + 1),
			Theme.mix(Theme.kind(o.getAfter().getResult().getKind()), Theme.TEXT_FAINT, lit ? 0 : 0.35));
	}

	/**
	 * A stop's name in at most two lines. A pair bought together ("A + B") is named by its first
	 * piece, whole; the second is drawn on the ring's shoulder and the price says it buys both,
	 * so the stop never reads like an upgrade level ("Obsidian platelegs +1").
	 */
	static List<String> nameLines(String name, FontMetrics fm, int width)
	{
		int plus = name.indexOf(" + ");
		return lines(plus < 0 ? name : name.substring(0, plus), fm, width, 2);
	}

	/** The stop's price, "9.9M", or "9.9M for both" for a pair when that fits under its ring. */
	static String priceLine(Roadmap.Option o, FontMetrics fm, int width)
	{
		String price = o.getCost() > 0 ? Format.coins(o.getCost()) : "free";
		String both = price + " for both";
		return o.getName().contains(" + ") && fm.stringWidth(both) <= width ? both : price;
	}

	/** The second piece of a pair bought together, or null for a single upgrade. */
	static Gear partner(Roadmap.Option o)
	{
		int plus = o.getName().indexOf(" + ");
		if (plus < 0)
		{
			return null;
		}
		String second = o.getName().substring(plus + 3);
		for (Gear g : o.getPieces())
		{
			if (g.getName().equals(second))
			{
				return g;
			}
		}
		return null;
	}

	private static String kindName(Result.Kind kind)
	{
		return kind == null ? "Melee" : kind.toString().substring(0, 1) + kind.toString().substring(1).toLowerCase(Locale.ROOT);
	}

	/** The savings goal as a ribbon: how far the coins go and, with Ledger, roughly how many days are left. */
	private static int savings(Graphics2D g, Savings.View v, int x, int y, int cw)
	{
		boolean done = v.isOwned() || v.isAffordable();
		String pace = done ? "" : v.getDays() > 0 ? "about " + v.getDays() + (v.getDays() == 1 ? " day" : " days") + " at my pace"
			: Format.coins(v.getRemaining()) + " to go";
		int h = pace.isEmpty() ? 34 : 48;
		RoundRectangle2D box = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, cw - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(x, y, Theme.CARD_TOP, x, y + h, Theme.CARD));
		g.fill(box);
		g.setColor(Theme.alpha(done ? Theme.TEAL : Theme.GOLD, 140));
		g.draw(box);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String tag = v.isOwned() ? "SAVED FOR" : "SAVING FOR";
		g.setColor(done ? Theme.TEAL : Theme.GOLD);
		g.drawString(tag, x + 9, y + 7 + small.getAscent());
		String pct = v.isOwned() ? "yours" : Math.round(v.getProgress() * 100) + "%";
		g.setColor(done ? Theme.TEAL : Theme.GOLD_BRIGHT);
		g.drawString(pct, x + cw - 9 - small.stringWidth(pct), y + 7 + small.getAscent());
		g.setFont(Theme.body());
		FontMetrics fm = g.getFontMetrics();
		int tx = x + 9 + small.stringWidth(tag) + 8;
		ProgressRing.drawShadowed(g, clip(v.getOption().getName(), fm, x + cw - 9 - small.stringWidth(pct) - 8 - tx), tx,
			y + 7 + small.getAscent(), Theme.TEXT);
		int by = y + 24;
		g.setColor(Theme.INK_DEEP);
		g.fill(new RoundRectangle2D.Float(x + 9, by, cw - 18, 4, 4, 4));
		g.setPaint(new GradientPaint(x + 9, 0, Theme.GOLD_DIM, x + cw - 9, 0, done ? Theme.TEAL : Theme.GOLD_BRIGHT));
		g.fill(new RoundRectangle2D.Float(x + 9, by, (float) Math.max(4, (cw - 18) * Math.min(1, v.isOwned() ? 1 : v.getProgress())), 4, 4, 4));
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_MUTED);
		if (!pace.isEmpty())
		{
			g.drawString(pace, x + cw - 9 - small.stringWidth(pace), by + 6 + small.getAscent());
		}
		return y + h;
	}

	private static void fit(Graphics2D g, BufferedImage icon, int x, int y, int w, int h, float alpha)
	{
		double scale = Math.min(1, Math.min(w / (double) Math.max(1, icon.getWidth()), h / (double) Math.max(1, icon.getHeight())));
		int dw = (int) Math.round(icon.getWidth() * scale);
		int dh = (int) Math.round(icon.getHeight() * scale);
		java.awt.Composite was = g.getComposite();
		if (alpha < 1)
		{
			g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
		}
		g.drawImage(icon, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
		g.setComposite(was);
	}

	private static void centred(Graphics2D g, String text, int left, int width, int baseline, Color color)
	{
		ProgressRing.drawShadowed(g, text, left + (width - g.getFontMetrics().stringWidth(text)) / 2, baseline, color);
	}

	private static void header(Graphics2D g, String text, String trailing, int x, int y, int w)
	{
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int mid = y + 10;
		diamond(g, x + 3, mid, 3, Theme.GOLD);
		String label = text.toUpperCase(Locale.ROOT);
		g.setColor(Theme.GOLD);
		g.drawString(label, x + 12, mid + fm.getAscent() / 2 - 1);
		int lineFrom = x + 12 + fm.stringWidth(label) + 6;
		int lineTo = x + w - (trailing == null ? 0 : fm.stringWidth(trailing) + 6);
		if (lineTo > lineFrom)
		{
			g.setPaint(new GradientPaint(lineFrom, 0, Theme.alpha(Theme.GOLD_DIM, 160), lineTo, 0, Theme.alpha(Theme.GOLD_DIM, 0)));
			g.fillRect(lineFrom, mid, lineTo - lineFrom, 1);
		}
		if (trailing != null)
		{
			g.setColor(Theme.TEXT_MUTED);
			g.drawString(trailing, x + w - fm.stringWidth(trailing), mid + fm.getAscent() / 2 - 1);
		}
	}

	private static void rule(Graphics2D g, int x, int y, int w)
	{
		g.setStroke(new BasicStroke(1f));
		g.setPaint(new GradientPaint(x, 0, Theme.alpha(Theme.GOLD, 0), x + w / 2f, 0, Theme.alpha(Theme.GOLD, 150), true));
		g.drawLine(x, y, x + w, y);
		diamond(g, x + w / 2f, y, 3, Theme.GOLD);
	}

	private static void diamond(Graphics2D g, float cx, float cy, float r, Color color)
	{
		Path2D d = new Path2D.Float();
		d.moveTo(cx, cy - r);
		d.lineTo(cx + r, cy);
		d.lineTo(cx, cy + r);
		d.lineTo(cx - r, cy);
		d.closePath();
		g.setColor(color);
		g.fill(d);
	}

	/** Text broken at spaces into at most a number of lines of that width, the last cut with an ellipsis if it runs on. */
	static List<String> lines(String text, FontMetrics fm, int width, int max)
	{
		List<String> out = new java.util.ArrayList<>();
		String rest = text == null ? "" : text.trim();
		while (!rest.isEmpty() && out.size() < max - 1 && fm.stringWidth(rest) > width)
		{
			int cut = -1;
			for (int i = rest.indexOf(' '); i > 0; i = rest.indexOf(' ', i + 1))
			{
				if (fm.stringWidth(rest.substring(0, i)) > width)
				{
					break;
				}
				cut = i;
			}
			if (cut < 0)
			{
				break;
			}
			out.add(rest.substring(0, cut));
			rest = rest.substring(cut + 1);
		}
		if (!rest.isEmpty())
		{
			out.add(clip(rest, fm, width));
		}
		return out;
	}

	static String clip(String text, FontMetrics fm, int width)
	{
		if (text == null)
		{
			return "";
		}
		if (fm.stringWidth(text) <= width)
		{
			return text;
		}
		int end = text.length();
		while (end > 0 && fm.stringWidth(text.substring(0, end) + "...") > width)
		{
			end--;
		}
		return text.substring(0, end) + "...";
	}
}
