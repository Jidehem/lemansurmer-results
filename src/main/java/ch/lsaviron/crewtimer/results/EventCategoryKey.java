package ch.lsaviron.crewtimer.results;

import java.util.Comparator;

record EventCategoryKey(EventId event, String category)
		implements Comparable<EventCategoryKey> {

	@Override
	public int compareTo(final EventCategoryKey o) {
		return Comparator.comparing(EventCategoryKey::event, EventId.COMPARATOR)
				.thenComparing(EventCategoryKey::category).compare(this, o);
	}

	public String toStandardCategory() {
		if (!isSwissChampionshipCategory()) {
			return category;
		}
		return category.substring(0, category.length() - 1);
	}

	final boolean isSwissChampionshipCategory() {
		return LSM.isSwissChampionship(category);
	}
}
