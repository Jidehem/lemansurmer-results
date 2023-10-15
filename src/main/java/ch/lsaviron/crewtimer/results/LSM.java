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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.util.StringUtil;

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
			int line = 1;
			for (final CSVRecord record : parser) {
				//System.out.println(record);
				if (record.size() != CsvResultHeaders.values().length) {
					throw new IOException(String.format(
							"Inconsistent number of fields in CSV line %d (%s)%nCheck that data in %s is consistent"
									+ " and/or class %s contains all header names",
							++line,
							record,
							resultsFromCrewTimerCsv,
							CsvResultHeaders.class.getSimpleName()));
				}
				String start = record.get(CsvResultHeaders.Start);
				if (start == null) {
					start = lastStart;
				} else {
					lastStart = start;
				}

				// FIXME remove debug
				printDiffBugColumns(record);
				// test delta bouées
				final var cr = new CategoryResult(extractEventNum(record),
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

	private void printDiffBugColumns(final CSVRecord record) {
		final String deltaBug = record.get(CsvResultHeaders
				.values()[CsvResultHeaders.Delta.ordinal() - 2]);
		final String crew = record.get(CsvResultHeaders.CrewAbbrev);
		final String eventNum = record.get(CsvResultHeaders.EventNum);
		final String event = record.get(CsvResultHeaders.Event);
		final String recordId = eventNum + " " + crew + " " + event;
		final String delta = record.get(CsvResultHeaders.Delta);
		if (!Objects.equals(deltaBug, delta) && deltaBug != null) {
			System.err.printf("d: %s / %s (%s)%n", deltaBug, delta, recordId);
		}

		final String adjTimeBug = record.get(CsvResultHeaders
				.values()[CsvResultHeaders.AdjTime.ordinal() - 2]);
		final String adjTime = record.get(CsvResultHeaders.AdjTime);
		if (!Objects.equals(adjTimeBug, adjTime) && !"DNS".equals(adjTime)
				&& !"DNF".equals(adjTime)) {
			System.err.printf("at: %s / %s (%s)%n",
					adjTimeBug,
					adjTime,
					recordId);
		}
	}

	private EventId extractEventNum(final CSVRecord record) {
		return EventId.from(record.get(CsvResultHeaders.EventNum));
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
				LocalTime finish = null;
				if (StringUtil.isNotBlank(categoryResult.finish)) {
					finish = LocalTime.parse(categoryResult.finish);
				}
				if (firstFinish == null) {
					firstFinish = finish;
					categoryResult.delta = null;
				} else if (finish == null) {
					// typically a DNS: do nothing

				} else {
					// adapt delta compared to first
					final Duration delta = Duration.between(firstFinish,
							finish);
					final LocalTime deltaLT = LocalTime.MIN.plus(delta);
					categoryResult.delta = deltaLT.format(DELTA_FORMATTER);
				}
				// compute rank in category
				if (finish == null || finish.compareTo(lastFinish) == 0) {
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
			//System.out.printf("-----%nevent category key: %s%n", res);
			String extraSwissChampionship = "";
			if (res.isSwissChampionshipCategory()) {
				extraSwissChampionship = " 🏆🇨🇭";
			}
			printHelper.printRaceHeader(res.toStandardCategory()
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
		if (start == null) {
			return "<startTime>";
		}
		if (start.equals("DNS")) {
			return start;
		}
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

	// duplicate/merge some special categories to have correct result
	private void mergeSpecialCategories(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results) {
		// copy since modified
		for (final Entry<EventCategoryKey, List<CategoryResult>> entry : new HashSet<>(
				results.entrySet())) {
			final EventCategoryKey eventCategoryKey = entry.getKey();
			// FIXME handle case of masters that should be merged too, but only for CH champ categories ?!?
			if (eventCategoryKey.isSwissChampionshipCategory()) {
				// add it to the corresponding non-swiss championship category
				final String standardCategory = eventCategoryKey
						.toStandardCategory();
				final List<List<CategoryResult>> standardRes = results
						.entrySet().stream().filter(e -> {
							final EventCategoryKey key = e.getKey();
							return standardCategory.equals(key.category())
									&& Objects.equals(
											eventCategoryKey.event().emoji(),
											key.event().emoji());
						}).map(e -> e.getValue()).toList();
				final List<CategoryResult> crs;
				if (standardRes.isEmpty()) {
					System.out.printf(
							"Info: no standard category found for %s%n",
							eventCategoryKey);
					// create corresponding category
					final EventCategoryKey eck = new EventCategoryKey(
							eventCategoryKey.event(), standardCategory);
					crs = new ArrayList<>();
					results.put(eck, crs);
				} else if (standardRes.size() > 1) {
					throw new RuntimeException("Found " + standardRes.size()
							+ " results while expecting only one for "
							+ eventCategoryKey);
				} else {
					crs = standardRes.get(0);
				}
				// we need to (deep-)copy each value since they will be modified
				entry.getValue().stream().map(CategoryResult::new)
						.forEachOrdered(crs::add);
				crs.sort(Comparator.comparing(cr -> cr.eventRank,
						Comparator.nullsLast(Comparator.naturalOrder())));
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
			// 3 medals are given, unless there is not enough participants
			return Math.min(nbParticipants, 3);
		}
		// LSM
		// 3 medals are given, but the last one must not receive a medal
		return Math.min(Math.max(0, nbParticipants - 1), 3);
	}

	static final boolean isSwissChampionship(final String category) {
		return category.endsWith("*");
	}

}
