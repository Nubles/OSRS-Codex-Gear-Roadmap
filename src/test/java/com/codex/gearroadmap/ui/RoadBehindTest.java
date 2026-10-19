package com.codex.gearroadmap.ui;

import com.codex.gearroadmap.core.Claims;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RoadBehindTest
{
	@Test
	public void eachClaimSaysWhatItAddedAndWhere()
	{
		assertEquals("+0.81 DPS at Vorkath", RoadmapView.behindDetail(new Claims.Claim("Ghrazi rapier", 1, 0.81, "Vorkath", 5)));
		assertEquals("Pair, +0.23 DPS at Vorkath",
			RoadmapView.behindDetail(new Claims.Claim("Obsidian platelegs + Inquisitor's great helm", 1, 0.23, "Vorkath", 5)));
		assertEquals("Obsidian platelegs", RoadmapView.firstPiece("Obsidian platelegs + Inquisitor's great helm"));
	}

	@Test
	public void theSectionListsTheLatestFiveAndNothingBeforeTheFirst()
	{
		StackPanel empty = new StackPanel(0, 0);
		RoadmapView.roadBehind(empty, Collections.emptyList(), null);
		assertEquals(0, empty.getComponentCount());
		StackPanel stack = new StackPanel(0, 0);
		Claims.Claim[] six = new Claims.Claim[6];
		for (int i = 0; i < six.length; i++)
		{
			six[i] = new Claims.Claim("Piece " + i, 1, 0.1, "Vorkath", 5 + i);
		}
		RoadmapView.roadBehind(stack, Arrays.asList(six), (id, cb) -> null);
		// The heading and five rows.
		assertEquals(1 + RoadmapView.BEHIND, stack.getComponentCount());
	}
}
