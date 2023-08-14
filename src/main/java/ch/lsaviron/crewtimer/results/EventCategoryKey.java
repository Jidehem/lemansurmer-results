package ch.lsaviron.crewtimer.results;

import java.util.Comparator;

record EventCategoryKey(int event, String category)
		implements Comparable<EventCategoryKey> {

	@Override
	public int compareTo(final EventCategoryKey o) {
		return Comparator.comparingInt(EventCategoryKey::event)
				.thenComparing(EventCategoryKey::category).compare(this, o);
	}

	public EventCategoryKey toStandardCategory() {
		if (!isSwissChampionshipCategory()) {
			return this;
		}
		return new EventCategoryKey(event,
				category.substring(0, category.length() - 1));
	}

	final boolean isSwissChampionshipCategory() {
		return LSM.isSwissChampionship(category);
	}
}
