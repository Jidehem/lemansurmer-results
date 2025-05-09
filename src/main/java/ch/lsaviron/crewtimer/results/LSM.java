package ch.lsaviron.crewtimer.results;

import java.io.FileNotFoundException;
// Run with:
// java -cp . LSM.java
//
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;

import com.google.common.annotations.VisibleForTesting;

import ch.lsaviron.lsm.LsmEventCategory;
import ch.lsaviron.swissrowing.AgeCategory;

/**
 * @author Jean-David Maillefer, 2023-2024
 */
public class LSM {

	public static int currentYear = 2024;

	private static final Map<Integer, List<String>> INTERMEDIATE_POINTS_CSV_HEADERS_PER_YEAR = Map
			.of(2023, List.of("Bouée_A", "Bouée_C"));

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

	/**
	 * Set the current year to the current system year.
	 */
	public static void resetCurrentYear() {
		currentYear = LocalDate.now().getYear();
	}

	public static void setCurrentYear(final int year) {
		currentYear = year;
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
		final PrintMode printMode;
		try {
			printMode = PrintMode.valueOf(args[1].toUpperCase(Locale.ROOT));
		} catch (final IllegalArgumentException iae) {
			System.err.printf("Print mode must be one of %s%n",
					Arrays.toString(PrintMode.values()));
			return;
		}

		new LSM(resultsFromCrewTimerCsv, printMode).processResults();
	}

	public static String normalize(final String s) {
		if (s == null) {
			return null;
		}
		return Normalizer.normalize(s, Form.NFC);
	}

	private void processResults() throws IOException {
		final var results = readRawResultsFromCsv();
		//System.out.println(results.keySet());
		mergeSpecialCategories(results);
		fixRankAndDelta(results);
		printResults(results);
	}

	private SortedMap<EventCategoryKey, List<CategoryResult>> readRawResultsFromCsv()
			throws IOException, FileNotFoundException {
		// manage headers: merge static ones with dynamic ones
		final List<String> intermediatePointsHeaders = INTERMEDIATE_POINTS_CSV_HEADERS_PER_YEAR
				.getOrDefault(currentYear, List.of());
		final List<String> headers1 = Arrays.stream(CsvResultHeaders.values())
				.map(h -> h.name()).toList();
		final int splitIndex = CsvResultHeaders.RawTime.ordinal();
		final String[] headers = Stream
				.concat(Stream.concat(headers1.subList(0, splitIndex).stream(),
						intermediatePointsHeaders.stream()),
						headers1.subList(splitIndex, headers1.size()).stream())
				.toArray(String[]::new);

		// read CSV
		try (final Reader in = new FileReader(resultsFromCrewTimerCsv)) {
			final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
					.setAllowMissingColumnNames(false).setSkipHeaderRecord(true)
					.setNullString("").setHeader(headers).build();
			final CSVParser parser = csvFormat.parse(in);
			//System.out.println(parser.getHeaderNames());

			final SortedMap<EventCategoryKey, List<CategoryResult>> results = new TreeMap<>();

			// workaround a bug in CrewTimer CSV: the disqualified teams have no start time
			String lastStart = null;
			int line = 1;
			for (final CSVRecord record : parser) {
				final Function<CsvResultHeaders, String> getData = header -> normalize(
						record.get(header));
				//System.out.println(record);
				if (record.size() != CsvResultHeaders.values().length
						+ intermediatePointsHeaders.size()) {
					throw new IOException(String.format(
							"Inconsistent number of fields in CSV line %d (%s)%nCheck that data in %s is consistent"
									+ " and/or class %s contains all header names",
							++line,
							record,
							resultsFromCrewTimerCsv,
							CsvResultHeaders.class.getSimpleName()));
				}
				String start = getData.apply(CsvResultHeaders.Start);
				if (start == null) {
					start = lastStart;
				} else {
					lastStart = start;
				}

				// intermediate times
				final Map<String, String> intermediateTimesByPoint = Collections
						.unmodifiableMap(intermediatePointsHeaders.stream()
								.collect(HashMap::new,
										(m, v) -> m.put(v, record.get(v)),
										HashMap::putAll));

				// test delta bouées
				final var cr = new CategoryResult(
						EventId.from(getData.apply(CsvResultHeaders.EventNum)),
						getData.apply(CsvResultHeaders.Event),
						Optional.ofNullable(
								getData.apply(CsvResultHeaders.Place))
								.map(Integer::parseInt).orElse(null),
						getData.apply(CsvResultHeaders.Crew),
						getData.apply(CsvResultHeaders.CrewAbbrev),
						getData.apply(CsvResultHeaders.Stroke), start,
						intermediateTimesByPoint,
						getData.apply(CsvResultHeaders.Finish),
						getData.apply(CsvResultHeaders.Delta),
						getData.apply(CsvResultHeaders.AdjTime));
				// debug infos
				//System.out.println(cr);
				try {
					results.computeIfAbsent(cr.getEventCategory(),
							k -> new ArrayList<>()).add(cr);
				} catch (final RuntimeException e) {
					// add more context
					System.err.printf("Error while parsing %s%n", cr);
					throw e;
				}
			}
			results.replaceAll((k, v) -> v.stream()
					// Sorted by construction/CSVstructure. But to be sure we sort.
					.sorted()
					// Using immutable list to avoid undesired modifications
					.toList());
			return results;
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
				LocalTime finishRaw = null;
				if (StringUtil.isNotBlank(categoryResult.finish)) {
					finishRaw = LocalTime.parse(categoryResult.finish);
				}
				LocalTime finish = null;
				if (StringUtil.isNotBlank(categoryResult.adjTime)
						&& !categoryResult.adjTime.equals("DNS")
						&& !categoryResult.adjTime.equals("DNF")) {
					finish = LocalTime.parse(categoryResult.start)
							.plus(parseDuration(categoryResult.adjTime));
					if (finishRaw != null && !finishRaw.equals(finish)) {
						System.out.printf(
								"Attention: la fin %s ne correspond pas à la fin ajustée %s (%s)%n",
								finishRaw,
								finish,
								categoryResult);
					}
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

	// TODO add unit test
	@VisibleForTesting
	static Duration parseDuration(final String time) {
		final Matcher matcher = Pattern.compile(
				"(?:([0-9]+):)?([0-9]{1,2}):([0-9]{2})(?:.([0-9]{1,3}))?")
				.matcher(time);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Time " + time + " is not a valide time representation");
		}
		Duration duration = Duration.ofMinutes(Long.parseLong(matcher.group(2)))
				.plus(Duration.ofSeconds(Long.parseLong(matcher.group(3))));

		final String groupHours = matcher.group(1);
		if (StringUtils.isNotEmpty(groupHours)) {
			duration = duration
					.plus(Duration.ofHours(Long.parseLong(groupHours)));
		}

		String groupMillis = matcher.group(4);
		if (StringUtils.isNotEmpty(groupMillis)) {
			while (groupMillis.length() < 3) {
				groupMillis += "0";
			}
			duration = duration
					.plus(Duration.ofMillis(Long.parseLong(groupMillis)));
		}

		return duration;
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
			final LsmEventCategory lsmEventCategory = LsmEventCategory
					.parse(res.category());
			//System.out.printf("-----%nevent category key: %s%n", res);
			String extraSwissChampionship = "";
			if (lsmEventCategory.swissChampionship()) {
				extraSwissChampionship = " 🏆🇨🇭";
			}
			printHelper.printRaceHeader(toStandardCategory(lsmEventCategory)
					+ extraSwissChampionship + " (course " + res.event() + ", "
					+ getStartTime(resCat.get(0).start) + ")");

			// race results
			final int nbMedals = getNbMedals(resCat.size(),
					lsmEventCategory.swissChampionship());
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

	public static String toStandardCategory(
			final LsmEventCategory lsmEventCategory) {
		final String res = lsmEventCategory.toString();
		if (!lsmEventCategory.swissChampionship()) {
			return res;
		}
		return res.substring(0, res.length() - 1);
	}

	// duplicate/merge some special categories to have correct result
	private void mergeSpecialCategories(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results) {
		// copy since modified
		final TreeSet<Entry<EventCategoryKey, List<CategoryResult>>> entrySet = new TreeSet<>(
				Entry.comparingByKey());
		entrySet.addAll(results.entrySet());
		for (final Entry<EventCategoryKey, List<CategoryResult>> entry : entrySet) {
			final EventCategoryKey eventCategoryKey = entry.getKey();
			//System.out.printf("Processing %s%n", eventCategoryKey);
			final LsmEventCategory lsmEventCategory = LsmEventCategory
					.parse(eventCategoryKey.category());

			mergeSwissChampionshipCategoryResultsIntoStandardOnes(results,
					eventCategoryKey,
					lsmEventCategory,
					entry.getValue());

			mergeMasterCategoryIntoStandardCategoryForOpen(results,
					eventCategoryKey,
					lsmEventCategory,
					entry.getValue());
		}

		// delete possibly empty results
		results.entrySet().removeIf(e -> e.getValue().isEmpty());
	}

	private void mergeSwissChampionshipCategoryResultsIntoStandardOnes(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results,
			final EventCategoryKey eventCategoryKey,
			final LsmEventCategory lsmEventCategory,
			final List<CategoryResult> categoryResultsBase) {
		// FIXME handle case of masters that should be merged too, but only for CH champ categories ?!?
		if (lsmEventCategory.swissChampionship()) {
			// add it to the corresponding non-swiss championship category
			final String standardCategory = toStandardCategory(
					lsmEventCategory);
			final List<Entry<EventCategoryKey, List<CategoryResult>>> standardRes = results
					.entrySet().stream().filter(e -> {
						final EventCategoryKey key = e.getKey();
						return standardCategory.equals(key.category())
								&& Objects.equals(
										eventCategoryKey.event().emoji(),
										key.event().emoji());
					}).toList();
			final List<CategoryResult> crs;
			final EventCategoryKey eckToUse;
			if (standardRes.isEmpty()) {
				System.out.printf(
						"Info: aucune catégorie standard trouvée pour '%s'%n",
						eventCategoryKey.category());
				// create corresponding category
				eckToUse = new EventCategoryKey(eventCategoryKey.event(),
						standardCategory);
				crs = List.of();
			} else if (standardRes.size() > 1) {
				throw new RuntimeException("Found " + standardRes.size()
						+ " results while expecting only one for "
						+ eventCategoryKey);
			} else {
				final Entry<EventCategoryKey, List<CategoryResult>> entry2 = standardRes
						.get(0);
				eckToUse = entry2.getKey();
				crs = entry2.getValue();
				System.out.printf(
						"Info: fusion de la catégorie '%s' dans la catégorie standard '%s'%n",
						eventCategoryKey.category(),
						eckToUse.category());
			}
			// we need to (deep-)copy each value since they will be modified
			final List<CategoryResult> categoryResults = Stream
					.concat(crs.stream(), categoryResultsBase.stream())
					// actual copy action
					.map(CategoryResult::new)
					.sorted(Comparator.comparing(cr -> cr.eventRank,
							Comparator.nullsLast(Comparator.naturalOrder())))
					.toList();
			results.put(eckToUse, categoryResults);
		}
	}

	// Cas particulier: pour l'open (LSM uniquement, pas championnats CH), les courses masters sont fusionnées avec les seniors
	// La clé des courses fusionnées est celle de la course sans Master
	private void mergeMasterCategoryIntoStandardCategoryForOpen(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results,
			final EventCategoryKey eventCategoryKey,
			final LsmEventCategory lsmEventCategory,
			final List<CategoryResult> categoryResults) {
		if (lsmEventCategory.open()) {
			final LsmEventCategory mergedLsmEventCategory = lsmEventCategory
					.withAgeCategory(AgeCategory.SENIOR);
			if (!mergedLsmEventCategory.equals(lsmEventCategory)) {
				// create corresponding standard category
				final EventCategoryKey seniorCategoryKey = new EventCategoryKey(
						eventCategoryKey.event(),
						mergedLsmEventCategory.toString());

				// merge only if both master and standard age category exists
				final List<CategoryResult> standardCategoryResults = results
						.get(seniorCategoryKey);
				if (standardCategoryResults != null) {
					System.out.printf(
							"Info: fusion de la catégorie '%s' dans '%s'%n",
							lsmEventCategory,
							mergedLsmEventCategory);
					final List<CategoryResult> mergedResults = Stream
							.concat(standardCategoryResults.stream(),
									categoryResults.stream())
							.sorted(Comparator.comparing(cr -> cr.eventRank,
									Comparator.nullsLast(
											Comparator.naturalOrder())))
							.toList();
					results.put(seniorCategoryKey, mergedResults);
					// removal duplicate entry due to copy
					results.remove(eventCategoryKey);
				} else {
					System.out.printf(
							"Info: pas de fusion de la catégorie '%s' dans '%s' puisque la catégorie senior n'existe pas%n",
							lsmEventCategory,
							mergedLsmEventCategory);
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
			// 3 medals are given, unless there is not enough participants
			return Math.min(nbParticipants, 3);
		}
		// LSM
		// 3 medals are given, but the last one must not receive a medal
		return Math.min(Math.max(0, nbParticipants - 1), 3);
	}

}
