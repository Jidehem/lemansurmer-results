package ch.lsaviron.swissrowing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EventCategoryTest {

	@Test
	final void testParseM1x() {
		final EventCategory eventCategory = EventCategory.parse("M 1x");
		assertEquals(new EventCategory(AgeCategory.SENIOR, SexCategory.MEN,
				RowingCategory.CLASSIC, 1, OarCategory.DOUBLE_SCULL, false),
				eventCategory);
	}

	@Test
	final void testParseMMixC4xPlus() {
		final EventCategory eventCategory = EventCategory.parse("MMix C4x+");
		assertEquals(new EventCategory(AgeCategory.MASTER, SexCategory.MIXED,
				RowingCategory.COASTAL, 4, OarCategory.DOUBLE_SCULL, true),
				eventCategory);
	}

	@Test
	final void testParseU19W_8Plus() {
		final EventCategory eventCategory = EventCategory.parse("U19W 8+");
		assertEquals(new EventCategory(AgeCategory.U19, SexCategory.WOMEN,
				RowingCategory.CLASSIC, 8, OarCategory.SINGLE_SCULL, true),
				eventCategory);
	}

	@Test
	final void testParseW_8() {
		assertThrows(IllegalArgumentException.class,
				() -> EventCategory.parse("W 8"));
	}

	@Test
	final void testToString() {
		assertEquals("M 1x", EventCategory.parse("M 1x").toString());
		assertEquals("MMix C4x+", EventCategory.parse("MMix C4x+").toString());
		assertEquals("U19W 8+", EventCategory.parse("U19W 8+").toString());
		assertEquals("W 8+", EventCategory.parse("W 8+").toString());
		assertEquals("Mix 2", EventCategory.parse("Mix 2-").toString());
	}

}
