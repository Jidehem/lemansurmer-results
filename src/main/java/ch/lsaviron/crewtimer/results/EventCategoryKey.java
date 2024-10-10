package ch.lsaviron.crewtimer.results;

import java.util.Comparator;

record EventCategoryKey(EventId event, String category)
		implements Comparable<EventCategoryKey> {

	@Override
	public int compareTo(final EventCategoryKey o) {
		return Comparator.comparing(EventCategoryKey::event, EventId.COMPARATOR)
				.thenComparing(EventCategoryKey::category).compare(this, o);
	}

	public EventCategoryKey withCategory(final String category) {
		return new EventCategoryKey(event, category);
	}

}
