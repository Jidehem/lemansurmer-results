package ch.lsaviron.lsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ch.lsaviron.swissrowing.AgeCategory;
import ch.lsaviron.swissrowing.EventCategory;
import ch.lsaviron.swissrowing.OarCategory;
import ch.lsaviron.swissrowing.RowingCategory;
import ch.lsaviron.swissrowing.SexCategory;

class LsmEventCategoryTest {

	@Test
	final void testParseM1x() {
		final LsmEventCategory eventCategory = LsmEventCategory.parse("M 1x");
		assertEquals(
				new LsmEventCategory(false,
						new EventCategory(AgeCategory.SENIOR, SexCategory.MEN,
								RowingCategory.CLASSIC, 1,
								OarCategory.DOUBLE_SCULL, false),
						false),
				eventCategory);
	}

	@Test
	final void testParseMMixC4xPlus() {
		final LsmEventCategory eventCategory = LsmEventCategory
				.parse("MMix C4x+*");
		assertEquals(
				new LsmEventCategory(false,
						new EventCategory(AgeCategory.MASTER, SexCategory.MIXED,
								RowingCategory.COASTAL, 4,
								OarCategory.DOUBLE_SCULL, true),
						true),
				eventCategory);
	}

	@Test
	final void testParseOpenW_8Plus() {
		final LsmEventCategory eventCategory = LsmEventCategory
				.parse("Open W 8+");
		assertEquals(
				new LsmEventCategory(true,
						new EventCategory(AgeCategory.SENIOR,
								SexCategory.WOMEN, RowingCategory.CLASSIC, 8,
								OarCategory.SINGLE_SCULL, true),
						false),
				eventCategory);
	}

	@Test
	final void testParseW_8() {
		assertThrows(IllegalArgumentException.class,
				() -> LsmEventCategory.parse("W 8"));
	}

	@Test
	final void testToString() {
		assertEquals("M 1x", LsmEventCategory.parse("M 1x").toString());
		assertEquals("MMix C4x+",
				LsmEventCategory.parse("MMix C4x+").toString());
		assertEquals("U19W 8+", LsmEventCategory.parse("U19W 8+").toString());
		assertEquals("W 8+", LsmEventCategory.parse("W 8+").toString());
		assertEquals("Mix 2", LsmEventCategory.parse("Mix 2-").toString());
	}

}
