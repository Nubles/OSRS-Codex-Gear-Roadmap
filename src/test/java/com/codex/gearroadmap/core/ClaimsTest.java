package com.codex.gearroadmap.core;

import com.google.gson.Gson;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ClaimsTest
{
	private static final long T = 1_800_000_000_000L;

	@Test
	public void newestFirstAndOneLineForEachUpgrade()
	{
		Claims claims = new Claims();
		claims.add(new Claims.Claim("Abyssal dagger", 13265, 0.15, "Vorkath", T));
		claims.add(new Claims.Claim("Ghrazi rapier", 22324, 0.81, "Vorkath", T + 1000));
		// Sold and bought back: it moves up rather than being listed twice.
		claims.add(new Claims.Claim("Abyssal dagger", 13265, 0.12, "Zulrah", T + 2000));
		List<Claims.Claim> recent = claims.recent();
		assertEquals(2, recent.size());
		assertEquals("Abyssal dagger", recent.get(0).getName());
		assertEquals("Zulrah", recent.get(0).getTarget());
		assertEquals("Ghrazi rapier", recent.get(1).getName());
	}

	@Test
	public void onlyTheLatestAreKept()
	{
		Claims claims = new Claims();
		for (int i = 0; i < Claims.KEPT + 5; i++)
		{
			claims.add(new Claims.Claim("Piece " + i, 1, 0.1, "Vorkath", T + i));
		}
		assertEquals(Claims.KEPT, claims.recent().size());
		assertEquals("Piece " + (Claims.KEPT + 4), claims.recent().get(0).getName());
	}

	@Test
	public void survivesTheSaveAndIgnoresBrokenLines()
	{
		Gson gson = new Gson();
		Claims claims = new Claims();
		claims.add(new Claims.Claim("Ghrazi rapier", 22324, 0.81, "Vorkath", T));
		claims.add(new Claims.Claim("", 1, 0.1, "Vorkath", T));
		claims.add(null);
		Claims back = gson.fromJson(gson.toJson(claims), Claims.class);
		assertEquals(1, back.recent().size());
		assertEquals(0.81, back.recent().get(0).getGain(), 1e-9);
		assertEquals(22324, back.recent().get(0).getIcon());
		// A hand-edited or older save with holes in it still reads.
		Claims odd = gson.fromJson("{\"claims\":[null,{\"name\":\"Fang\",\"icon\":2,\"gain\":0.3,\"target\":\"Vorkath\",\"at\":5},{\"name\":\"Bad\"}]}", Claims.class);
		assertEquals(1, odd.recent().size());
		assertTrue(gson.fromJson("{}", Claims.class).isEmpty());
		Claims nulls = gson.fromJson("{\"claims\":null}", Claims.class);
		nulls.add(new Claims.Claim("Fang", 2, 0.3, "Vorkath", T));
		assertEquals(1, nulls.recent().size());
	}
}
