package ch.lsaviron.crewtimer.results;

import java.util.Comparator;

class CategoryResult implements Comparable<CategoryResult>, Cloneable {

	final EventId event;

	final String eventName;

	final Integer eventRank;

	final String crew;

	final String crewAbbrev;

	final String category;

	final String start;

	final String finish;

	String delta;

	Integer categoryRank;

	final String adjTime;

	CategoryResult(final EventId event, final String eventName,
			final Integer eventRank, final String crew, final String crewAbbrev,
			final String category, final String start, final String finish,
			final String delta, final String adjTime) {
		this.event = event;
		this.eventName = eventName;
		this.eventRank = eventRank;
		this.crew = crew;
		this.crewAbbrev = crewAbbrev;
		this.category = category;
		this.start = start;
		this.finish = finish;
		this.delta = delta;
		this.adjTime = adjTime;
	}

	/**
	 * Copy constructor.
	 *
	 * @param base
	 */
	CategoryResult(final CategoryResult base) {
		this(base.event, base.eventName, base.eventRank, base.crew,
				base.crewAbbrev, base.category, base.start, base.finish,
				base.delta, base.adjTime);
		categoryRank = base.categoryRank;
	}

	@Override
	public int compareTo(final CategoryResult o) {
		return Comparator
				.comparing((final CategoryResult cr) -> cr.event,
						EventId.COMPARATOR)
				.thenComparing(cr -> cr.category)
				.thenComparing(Comparator.comparing(
						(final CategoryResult cr) -> cr.eventRank,
						Comparator.nullsLast(Comparator.naturalOrder())))
				.thenComparing(Comparator.comparing(cr -> cr.finish,
						Comparator.nullsLast(Comparator.naturalOrder())))
				.compare(this, o);
	}

	final boolean isSwissChampionship() {
		return LSM.isSwissChampionship(category);
	}

	EventCategoryKey getEventCategory() {
		return new EventCategoryKey(event, category);
	}
}