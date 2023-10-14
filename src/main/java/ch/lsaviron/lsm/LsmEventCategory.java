package ch.lsaviron.lsm;

import java.util.Comparator;

import com.google.common.collect.ImmutableList;

import ch.lsaviron.swissrowing.EventCategory;

public record LsmEventCategory(boolean open, EventCategory eventCategory,
		boolean swissChampionship) {

	private static final String OPEN_CATEGORY = "Open ";

	private static final String SWISS_CHAMPIONSHIP_CATEGORY = "*";

	public static final ImmutableList<LsmEventCategory> EVENT_CATEGORIES = ImmutableList
			.of(parse("M C1x"),
					parse("M C1x*"),
					parse("M C2x"),
					parse("M C4x+"),
					parse("Mix C2x"),
					parse("Mix C2x*"),
					parse("Mix C4x+"),
					parse("MM C1x"),
					parse("MM C2x"),
					parse("MM C4x+"),
					parse("MMix C2x"),
					parse("MW C2x"),
					parse("MW C4x+"),
					parse("Open M C1x"),
					parse("Open M C2x"),
					parse("Open Mix C4x+"),
					parse("Open MM C1x"),
					parse("Open MM C4x+"),
					parse("Open MMix C2x"),
					parse("Open MMix C4x+"),
					parse("Open MW C4x+"),
					parse("Open W C4x+"),
					parse("U19M C1x*"),
					parse("U19M C2x"),
					parse("U19M C4x+"),
					parse("U19Mix C2x*"),
					parse("U19W C1x*"),
					parse("U19W C2x"),
					parse("U19W C4x+"),
					parse("W C1x*"),
					parse("W C2x"),
					parse("W C4x+"));

	public static final Comparator<LsmEventCategory> BY_EXPECTED_SPEED_COMPARATOR = Comparator
			.comparing(LsmEventCategory::eventCategory,
					EventCategory.BY_EXPECTED_SPEED_COMPARATOR);

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		if (open) {
			sb.append(OPEN_CATEGORY);
		}
		sb.append(eventCategory);
		if (swissChampionship) {
			sb.append(SWISS_CHAMPIONSHIP_CATEGORY);
		}
		return sb.toString();
	}

	public static Comparator<LsmEventCategory> EXPECTED_SPEED_COMPARATOR = Comparator
			.comparing(LsmEventCategory::eventCategory,
					EventCategory.EXPECTED_SPEED_COMPARATOR)
			.thenComparing(LsmEventCategory::swissChampionship,
					Comparator.reverseOrder());

	public static LsmEventCategory parse(final String raw) {
		int startIdx = 0;
		int endIdx = raw.length();
		boolean open = false;
		if (raw.startsWith(OPEN_CATEGORY)) {
			open = true;
			startIdx = OPEN_CATEGORY.length();
		}
		boolean swissChampionship = false;
		if (raw.endsWith(SWISS_CHAMPIONSHIP_CATEGORY)) {
			swissChampionship = true;
			endIdx -= 1;
		}
		return new LsmEventCategory(open,
				EventCategory.parse(raw.substring(startIdx, endIdx)),
				swissChampionship);
	}
}
