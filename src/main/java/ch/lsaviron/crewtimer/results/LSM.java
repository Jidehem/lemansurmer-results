package ch.lsaviron.crewtimer.results;

// Run with:
// java -cp . LSM.java
//
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * @author Jean-David Maillefer, 2023
 */
public class LSM {

	private static final DateTimeFormatter DELTA_FORMATTER = DateTimeFormatter
			.ofPattern("mm:ss.S");

	private static final DateTimeFormatter RACE_TIME_FORMATTER = DateTimeFormatter
			.ofPattern("HH'h'mm");

	final String resultsFromCrewTimerCsv;

	private final PrintMode printMode;

	public LSM(final String resultsFromCrewTimerCsv,
			final PrintMode printMode) {
		this.resultsFromCrewTimerCsv = resultsFromCrewTimerCsv;
		this.printMode = printMode;
	}

	public static void main(final String... args) throws Exception {
		// to avoid a log4j2 warning at startup
		System.setProperty("log4j2.loggerContextFactory",
				"org.apache.logging.log4j.simple.SimpleLoggerContextFactory");

		if (args.length != 2) {
			System.err.println(
					"Syntax: java LSM.java results-from-crewtimer.csv mode\n"
							+ "       where mode is SCREEN, TSV, XLS or XLSX");
			return;
		}
		final String resultsFromCrewTimerCsv = args[0];
		final PrintMode printMode = PrintMode
				.valueOf(args[1].toUpperCase(Locale.ENGLISH));

		new LSM(resultsFromCrewTimerCsv, printMode).processResults();
	}

	private void processResults() throws IOException {
		CSVParser parser;
		// read CSV:
		try (Reader in = new FileReader(resultsFromCrewTimerCsv)) {
			final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
					.setAllowMissingColumnNames(false).setSkipHeaderRecord(true)
					.setNullString("").setHeader(CsvResultHeaders.class)
					.build();
			parser = csvFormat.parse(in);
			//System.out.println(parser.getHeaderNames());

			final SortedMap<EventCategoryKey, List<CategoryResult>> results = new TreeMap<>();

			// workaround a bug in CrewTimer CSV: the disqualified teams have no start time
			String lastStart = null;
			for (final CSVRecord record : parser) {
				//System.out.println(record);
				String start = record.get(CsvResultHeaders.Start);
				if (start == null) {
					start = lastStart;
				} else {
					lastStart = start;
				}
				final var cr = new CategoryResult(
						Integer.parseInt(record.get(CsvResultHeaders.EventNum)),
						record.get(CsvResultHeaders.Event),
						Optional.ofNullable(record.get(CsvResultHeaders.Place))
								.map(Integer::parseInt).orElse(null),
						record.get(CsvResultHeaders.Crew),
						record.get(CsvResultHeaders.CrewAbbrev),
						record.get(CsvResultHeaders.Stroke), start,
						record.get(CsvResultHeaders.Finish),
						record.get(CsvResultHeaders.Delta),
						record.get(CsvResultHeaders.AdjTime));
				results.computeIfAbsent(cr.getEventCategory(),
						k -> new ArrayList<>()).add(cr);
			}
			// sorted by construction/CSVstructure. But to be sure
			results.values().forEach(v -> v.sort(null));

			mergeSpecialCategories(results);
			fixRankAndDelta(results);

			printResults(results);
		}
	}

	private void fixRankAndDelta(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results) {
		for (final List<CategoryResult> crs : results.values()) {
			int nb = 0;
			final int lastRank = 0;
			final LocalTime lastFinish = LocalTime.MIN;
			LocalTime firstFinish = null;
			for (final CategoryResult categoryResult : crs) {
				nb++;
				final LocalTime finish = LocalTime.parse(categoryResult.finish);
				if (firstFinish == null) {
					firstFinish = finish;
					categoryResult.delta = null;
				} else {
					// adapt delta compared to first
					final Duration delta = Duration.between(firstFinish,
							finish);
					final LocalTime deltaLT = LocalTime.MIN.plus(delta);
					categoryResult.delta = deltaLT.format(DELTA_FORMATTER);
				}
				// compute rank in category
				if (finish.compareTo(lastFinish) == 0) {
					// equality
					categoryResult.categoryRank = lastRank;
				} else {
					// next rank
					categoryResult.categoryRank = nb;
				}
			}
		}
	}

	private void printResults(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results) {
		final PrintHelper printHelper = printMode.buildHelper(this);
		for (final SubResult subResult : printHelper.getSubResults(results)) {
			subResult.init();
			final SortedMap<EventCategoryKey, List<CategoryResult>> results2 = subResult
					.getResults();
			//System.out.println(results2.size());
			printResults(results2, printHelper);
			subResult.end();
		}

		printHelper.end();
	}

	private void printResults(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results,
			final PrintHelper printHelper) {
		for (final Entry<EventCategoryKey, List<CategoryResult>> entry : results
				.entrySet()) {
			final List<CategoryResult> resCat = entry.getValue();

			// race header
			final EventCategoryKey res = entry.getKey();
			String extraSwissChampionship = "";
			if (res.isSwissChampionshipCategory()) {
				extraSwissChampionship = " 🏆🇨🇭";
			}
			printHelper.printRaceHeader(res.toStandardCategory().category()
					+ extraSwissChampionship + " (course " + res.event() + ", "
					+ getStartTime(resCat.get(0).start) + ")");

			// race results
			final int nbMedals = getNbMedals(resCat.size(),
					res.isSwissChampionshipCategory());
			for (final CategoryResult cr : resCat) {
				final String medals = getMedal(cr.categoryRank, nbMedals);
				printHelper.printResultRow(cr.categoryRank,
						medals,
						cr.crewAbbrev,
						cr.crew,
						cr.adjTime,
						cr.delta);
			}
			printHelper.printRaceFooter();
		}
	}

	private String getStartTime(final String start) {
		LocalTime time = LocalTime.parse(start);
		int minute = time.getMinute();
		minute = ((int) Math.round(minute / 5.0)) * 5;
		if (minute == 60) {
			time = time.plusHours(1);
		} else {
			time = time.withMinute(minute);
		}
		return RACE_TIME_FORMATTER.format(time);
	}

	private void mergeSpecialCategories(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results) {
		for (final Entry<EventCategoryKey, List<CategoryResult>> entry : results
				.entrySet()) {
			final EventCategoryKey eventCategoryKey = entry.getKey();
			if (eventCategoryKey.isSwissChampionshipCategory()) {
				// add it to the corresponding non-swiss championship category
				final List<CategoryResult> standardRes = results
						.get(eventCategoryKey.toStandardCategory());
				if (standardRes == null) {
					System.out
							.println("Warning: no standard category found for "
									+ eventCategoryKey);
				} else {
					// we need to (deep-)copy each value since they will be modified
					entry.getValue().stream().map(CategoryResult::new)
							.forEachOrdered(standardRes::add);
					standardRes
							.sort(Comparator.comparingInt(cr -> cr.eventRank));
				}
			}
		}
	}

	private String getMedal(final int rank, final int maxRankWithMedal) {
		if (rank > maxRankWithMedal) {
			return "";
		}
		switch (rank) {
		case 1:
			return "🥇";
		case 2:
			return "🥈";
		case 3:
			return "🥉";
		}
		return "";
	}

	private static int getNbMedals(final int nbParticipants,
			final boolean isSwissChampionship) {
		if (isSwissChampionship) {
			return Math.min(nbParticipants, 3);
		}
		// LSM
		return Math.min(Math.max(0, nbParticipants - 1), 3);
	}

	static final boolean isSwissChampionship(final String category) {
		return category.endsWith("*");
	}

}
