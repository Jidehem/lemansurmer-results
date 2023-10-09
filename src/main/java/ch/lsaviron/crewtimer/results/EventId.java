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

	public static enum Race {

		// declaration order matters
		ANEMONE("🪸"),
		BERNARD_LHERMITE("🐚"),
		CALAMAR("🦑"),
		DORADE("🐟"),
		// multiple encoding found for etoile character. 2b50 and fe0f
		ETOILE_DE_MER("\u2b50"),
		ETOILE_DE_MER_B("\u2b50\u2b50");

		private static final Map<String, Race> REVERSE = Maps
				.uniqueIndex(Arrays.asList(Race.values()), Race::getEmoji);

		private final String emoji;

		private Race(final String emoji) {
			if (StringUtil.isBlank(emoji)) {
				throw new IllegalArgumentException(
						"blank emoji is not allowed");
			}
			this.emoji = emoji;
		}

		public static Race fromEmoji(final String emoji) {
			if (emoji == null) {
				// special case to allow null emoji
				return null;
			}
			final Race res = REVERSE.get(emoji);
			if (res == null) {
				// debug infos
				System.err.println("emoji chars");
				emoji.chars().forEach(c -> System.err.printf("%x%n", c));

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
