package ch.lsaviron.lsm;

import java.util.Comparator;

import ch.lsaviron.swissrowing.EventCategory;

public record LsmEventCategory(boolean open, EventCategory eventCategory,
		boolean swissChampionship) {

	private static final String OPEN_CATEGORY = "Open ";

	private static final String SWISS_CHAMPIONSHIP_CATEGORY = "*";

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
