package ch.lsaviron.crewtimer.results;

import java.util.List;
import java.util.SortedMap;

/**
 * @author Jean-David Maillefer
 */
interface PrintHelper {

	void printRaceHeader(String header);

	void printResultRow(Integer categoryRank, String medals, String crewAbbrev,
			String crew, String adjTime, String delta);

	void printRaceFooter();

	default void end() {
		// do nothing by default
	}

	public static String formatDelta(final String value) {
		if (value == null) {
			return "";
		}
		return "+" + value;
	}

	List<SubResult> getSubResults(
			SortedMap<EventCategoryKey, List<CategoryResult>> results);

	static String formatAdjTime(final String adjTime) {
		if (adjTime == null) {
			return "(inconnu)";
		}
		return adjTime;
	}

	static String formatRank(final Integer rank) {
		if (rank == null || rank.intValue() == 0) {
			return "";
		}
		return "" + rank;
	}
}