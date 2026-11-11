package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.Fixtures;
import com.codex.gearroadmap.core.Advisor;
import com.codex.gearroadmap.core.Report;
import com.codex.gearroadmap.core.Roadmap;
import com.codex.gearroadmap.data.RoadmapData;
import com.codex.gearroadmap.overlay.UpgradePlaque;
import com.google.gson.Gson;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import javax.swing.JLabel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Nothing Gear Roadmap draws may clear the clip it was handed, or leave the composite, stroke,
 * font, transform or hints changed: RuneLite's overlay renderer goes on drawing with the same
 * graphics, and a cleared clip there broke every frame.
 */
public class ClipKeptTest
{
	private static RoadmapData data;
	private static Roadmap road;

	@BeforeClass
	public static void load() throws IOException
	{
		data = RoadmapData.load(new Gson());
		road = new Advisor(data).plan(Fixtures.midGame(data), Fixtures.midGameFighter(), data.target("Vorkath"), Fixtures.prices(data),
			23_400_000L);
	}

	/** The state a renderer hands over, and a check that it is all still there afterwards. */
	private static final class Handed
	{
		final BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		final Shape clip;
		final Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f);
		final Stroke stroke = new BasicStroke(2f);
		final Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
		final AffineTransform transform = AffineTransform.getTranslateInstance(7, 5);
		final Object antialias = RenderingHints.VALUE_ANTIALIAS_OFF;

		Handed()
		{
			g.setTransform(transform);
			g.setClip(new Rectangle(3, 4, 250, 180));
			clip = g.getClip();
			g.setComposite(composite);
			g.setStroke(stroke);
			g.setFont(font);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
		}

		void unchanged()
		{
			clipKept();
			assertEquals(composite, g.getComposite());
			assertEquals(stroke, g.getStroke());
			assertEquals(font, g.getFont());
			assertEquals(antialias, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
			g.dispose();
		}

		/** For a Swing component, whose own paint sets the font and colour on what it is given. */
		void clipKept()
		{
			assertNotNull("the clip was cleared", g.getClip());
			assertEquals(clip.getBounds(), g.getClip().getBounds());
			assertEquals(transform, g.getTransform());
		}
	}

	@Test
	public void thePlaqueLeavesTheOverlayGraphicsAsItFoundThem()
	{
		UpgradePlaque plaque = new UpgradePlaque();
		plaque.show(new UpgradePlaque.View("UPGRADE CLAIMED", "Abyssal tentacle", "+0.62 DPS", "Next: Bandos tassets", Theme.GOLD, null,
			System.currentTimeMillis() - 400, 8_000));
		Handed handed = new Handed();
		assertNotNull(plaque.render(handed.g));
		handed.unchanged();
	}

	@Test
	public void aCardWithAnAccentKeepsTheClip()
	{
		Card card = new Card(new java.awt.BorderLayout());
		card.accent(Color.RED);
		card.add(new JLabel("Vorkath"));
		card.setSize(200, 40);
		card.doLayout();
		Handed handed = new Handed();
		card.paint(handed.g);
		handed.clipKept();
	}

	@Test
	public void theRoadChartKeepsTheClip()
	{
		RoadChart chart = new RoadChart(road, (id, cb) -> null);
		chart.setSize(chart.getPreferredSize().width == 0 ? 220 : chart.getPreferredSize().width, chart.getPreferredSize().height);
		Handed handed = new Handed();
		chart.paint(handed.g);
		handed.clipKept();
	}

	@Test
	public void theRoadCardDrawsAWholePageWithoutClearingItsClip()
	{
		Report report = new Report(true, road, Collections.emptyList(), "Potions", 40, true).withAccount(false, false, null, null);
		BufferedImage card = RoadCard.render(report, "Zezima", null, 1_800_000_000_000L, null, false);
		assertNotNull(card);
	}
}
