package com.codex.gearroadmap.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormatTest
{
	@Test
	public void amountsFollowTheSuiteRule()
	{
		// In full below 10,000; whole k below a million; M to one decimal; B to two.
		assertEquals("8,240", Format.coins(8_240));
		assertEquals("10k", Format.coins(10_000));
		assertEquals("41k", Format.coins(41_200));
		assertEquals("96k", Format.coins(95_700));
		assertEquals("412k", Format.coins(412_000));
		assertEquals("1.2M", Format.coins(1_240_000));
		assertEquals("2.4M", Format.coins(2_370_000));
		assertEquals("26.0M", Format.coins(26_000_000));
		assertEquals("98.7M", Format.coins(98_700_000));
		assertEquals("812.4M", Format.coins(812_400_000));
		assertEquals("2.10B", Format.coins(2_100_000_000L));
		// Rounding that reaches the next unit is written in it.
		assertEquals("100k", Format.coins(99_960));
		assertEquals("1.0M", Format.coins(999_600));
		assertEquals("999k", Format.coins(999_499));
		assertEquals("1.00B", Format.coins(999_960_000));
		assertEquals("Free", Format.coins(0));
	}

}
