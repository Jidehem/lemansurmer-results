package ch.lsaviron.crewtimer.results;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

class CategoryResult implements Comparable<CategoryResult>, Cloneable {

	final EventId event;

	final String eventName;

	final Integer eventRank;

	final String crew;

	final String crewAbbrev;

	final String category;

	final String start;

	final Map<String, String> intermediateTimesByPoint;

	final String finish;

	String delta;

	Integer categoryRank;

	final String adjTime;

	CategoryResult(final EventId event, final String eventName,
			final Integer eventRank, final String crew, final String crewAbbrev,
			final String category, final String start,
			final Map<String, String> intermediateTimesByPoint,
			final String finish, final String delta, final String adjTime) {
		this.event = event;
		this.eventName = eventName;
		this.eventRank = eventRank;
		this.crew = crew;
		this.crewAbbrev = crewAbbrev;
		this.category = category;
		this.start = start;
		this.intermediateTimesByPoint = intermediateTimesByPoint;
		this.finish = finish;
		this.delta = delta;
		this.adjTime = adjTime;

		// fix adjTime after penalties
	}

	/**
	 * Copy constructor.
	 *
	 * @param base
	 */
	CategoryResult(final CategoryResult base) {
		this(base.event, base.eventName, base.eventRank, base.crew,
				base.crewAbbrev, base.category, base.start,
				base.intermediateTimesByPoint, base.finish, base.delta,
				base.adjTime);
		categoryRank = base.categoryRank;
	}

	@Override
	public int compareTo(final CategoryResult o) {
		return Comparator
				.<CategoryResult, EventId>comparing(cr -> cr.event,
						EventId.COMPARATOR)
				.thenComparing(cr -> cr.category)
				.thenComparing(Comparator.comparing(cr -> cr.eventRank,
						Comparator.nullsLast(Comparator.naturalOrder())))
				.thenComparing(Comparator.comparing(cr -> cr.finish,
						Comparator.nullsLast(Comparator.naturalOrder())))
				.compare(this, o);
	}

	EventCategoryKey getEventCategory() {
		return new EventCategoryKey(event, category);
	}

	@Override
	public String toString() {
		return "CategoryResult [event=" + event + ", eventName=" + eventName
				+ ", eventRank=" + eventRank + ", crew=" + crew
				+ ", crewAbbrev=" + crewAbbrev + ", category=" + category
				+ ", start=" + start + ", finish=" + finish + ", delta=" + delta
				+ ", categoryRank=" + categoryRank + ", adjTime=" + adjTime
				+ intermediateTimesByPoint.entrySet().stream().map(
						entry -> ", " + entry.getKey() + "=" + entry.getValue())
						.collect(Collectors.joining())
				+ "]";
	}

}