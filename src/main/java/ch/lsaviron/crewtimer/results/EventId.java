package ch.lsaviron.crewtimer.results;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.util.StringUtil;

import com.google.common.collect.Maps;

public record EventId(String emoji, int id) {

	public static final Comparator<EventId> COMPARATOR = Comparator
			.comparing((final EventId eid) -> Race.fromEmoji(eid.emoji),
					Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(EventId::id,
					Comparator.nullsFirst(Comparator.naturalOrder()));

	// TODO nice-to-have make this more generic across years ?
	public static enum Race {

		// declaration order matters
		// 2023 race IDs
		ANEMONE("🪸", 2023),
		BERNARD_LHERMITE("🐚", 2023),
		CALAMAR("🦑", 2023),
		DORADE("🐟", 2023),
		// multiple encoding found for etoile character. 2b50 and fe0f
		ETOILE_DE_MER("\u2b50", 2023),
		ETOILE_DE_MER_B("\u2b50\u2b50", 2023),

		// 2024 race IDs
		LAMANTIN("🦭", 2024),
		ECREVISSE("🦞", 2024),
		MANCHOT("🐧", 2024),
		ANCHOIS("🐟", 2024),
		NARVAL("🦄", 2024),
		NARVAL_B("🦄🦄", 2024),;

		private static final Map<String, Race> getReverse() {
			return Maps.uniqueIndex(
					Arrays.stream(Race.values())
							.filter(r -> r.order == LSM.currentYear).iterator(),
					Race::getEmoji);
		}

		private final String emoji;

		private final int order;

		private Race(final String emoji, final int order) {
			if (StringUtil.isBlank(emoji)) {
				throw new IllegalArgumentException(
						"blank emoji is not allowed");
			}
			this.emoji = LSM.normalize(emoji);
			this.order = order;
		}

		public static Race fromEmoji(final String emoji) {
			if (emoji == null) {
				// special case to allow null emoji
				return null;
			}
			final Race res = getReverse().get(LSM.normalize(emoji));
			if (res == null) {
				// debug infos
				System.err.println("emoji chars");
				emoji.chars().forEach(c -> System.err.printf("%x%n", c));
				System.err.println("normalized emoji chars");
				LSM.normalize(emoji).chars()
						.forEach(c -> System.err.printf("%x%n", c));

				throw new IllegalArgumentException(
						"Emoji " + emoji + " is not a known race marker");
			}
			return res;
		}

		public String getEmoji() {
			return emoji;
		}
	}

	private final static Pattern EVENT_ENUM_PATTERN_2023 = Pattern
			.compile("(.+) (\\d+)");

	private final static Pattern EVENT_ENUM_PATTERN_2022 = Pattern
			.compile("(\\d+)");

	public static EventId from(final String eventNumRaw) {
		Matcher matcher = EVENT_ENUM_PATTERN_2022.matcher(eventNumRaw);
		if (matcher.matches()) {
			return new EventId(null, Integer.parseInt(matcher.group(1)));
		}
		matcher = EVENT_ENUM_PATTERN_2023.matcher(eventNumRaw);
		if (matcher.matches()) {
			return new EventId(matcher.group(1),
					Integer.parseInt(matcher.group(2)));
		}
		throw new RuntimeException(
				"Failed to detect pattern for eventId " + eventNumRaw);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		if (emoji != null) {
			sb.append(emoji);
			sb.append(' ');
		}
		sb.append(id);
		return sb.toString();
	}

}
