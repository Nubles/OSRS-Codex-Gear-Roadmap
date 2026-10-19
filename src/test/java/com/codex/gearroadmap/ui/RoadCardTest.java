package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.core.Advisor;
import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.data.RoadmapData;
import com.google.gson.Gson;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/** The road card: its words, and that it draws a whole page for a real road and for an empty one. */
public class RoadCardTest
{
	private static final long NOW = 1_800_000_000_000L;
	private static RoadmapData data;
	private static Advisor advisor;
	private static Roadmap vorkath;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		advisor = new Advisor(data);
		vorkath = advisor.plan(Fixtures.midGame(data), Fixtures.midGameFighter(), data.target("Vorkath"), Fixtures.prices(data), 23_400_000L);
	}

	@Test
	public void titleNamesThePlayerAndTheTarget()
	{
		assertEquals("Zezima's road to Vorkath", RoadCard.title("Zezima", data.target("Vorkath")));
		assertEquals("My road to Vorkath", RoadCard.title(null, data.target("Vorkath")));
	}

	@Test
	public void verdictTellsTheWholeRoadsGainAndCost()
	{
		long total = vorkath.getPath().get(vorkath.getPath().size() - 1).getSpent();
		long share = Math.round((vorkath.finalDps() / vorkath.getNow().dps() - 1) * 100);
		assertEquals("+" + share + "% damage for " + Format.coins(total), RoadCard.verdict(vorkath, false));
		assertTrue(RoadCard.verdict(vorkath, true).contains("for gear worth "));
	}

	@Test
	public void aPairIsNamedByItsFirstPiece()
	{
		assertEquals("Obsidian platelegs +1", RoadCard.shortName("Obsidian platelegs + Inquisitor's great hammer"));
		assertEquals("Ghrazi rapier", RoadCard.shortName("Ghrazi rapier"));
	}

	@Test
	public void namesTakeTwoLinesAtMost()
	{
		FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().getFontMetrics(new Font(Font.MONOSPACED, Font.PLAIN, 10));
		int w = fm.stringWidth("Dragon hunter");
		assertEquals(Arrays.asList("Dragon hunter", "lance"), RoadCard.lines("Dragon hunter lance", fm, w, 2));
		assertEquals(Collections.singletonList("Fang"), RoadCard.lines("Fang", fm, w, 2));
		assertTrue(RoadCard.lines("Dragon hunter crossbow of the very long name", fm, w, 2).get(1).endsWith("..."));
		assertEquals(2, RoadCard.lines("Dragon hunter crossbow of the very long name", fm, w, 2).size());
	}

	@Test
	public void drawsOnlyWithARoad()
	{
		assertFalse(RoadCard.canDraw(Report.loggedOut()));
		assertFalse(RoadCard.canDraw(new Report(true, null, Collections.emptyList(), "Potions", 0, false)));
		assertTrue(RoadCard.canDraw(new Report(true, vorkath, Collections.emptyList(), "Potions", 40, true)));
	}

	@Test
	public void rendersAWholePageAtTwiceScale()
	{
		AtomicInteger asked = new AtomicInteger();
		Report report = new Report(true, vorkath, Collections.emptyList(), "Potions", 40, true);
		BufferedImage card = RoadCard.render(report, "Zezima", (id, cb) ->
		{
			asked.incrementAndGet();
			return null;
		}, NOW, () -> { });
		assertEquals(RoadCard.WIDTH * RoadCard.SCALE, card.getWidth());
		assertTrue(card.getHeight() > 400 * RoadCard.SCALE);
		// Every worn piece and every stop asked for its picture.
		assertTrue(asked.get() >= Math.min(RoadCard.STOPS, vorkath.getPath().size()));

		Roadmap bis = advisor.plan(Fixtures.midGame(data), Fixtures.midGameFighter(), data.target("Giant Mole"), id -> 0L, 0L);
		BufferedImage empty = RoadCard.render(new Report(true, bis, Collections.emptyList(), null, 40, true).withAccount(true, false, null, null),
			null, null, NOW, () -> { });
		assertTrue(empty.getHeight() < card.getHeight());
	}
}
