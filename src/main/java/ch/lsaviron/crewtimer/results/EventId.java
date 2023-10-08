package ch.lsaviron.crewtimer.results;

import java.util.Comparator;

public record EventId(String emoji, String letter, Integer id) {

	public static final Comparator<EventId> COMPARATOR = Comparator
			.comparing(EventId::id,
					Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(EventId::letter,
					Comparator.nullsFirst(Comparator.naturalOrder()));

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		if (emoji != null && letter != null) {
			sb.append(emoji);
			sb.append(' ');
			sb.append(letter);
		}
		if (id != null) {
			if (sb.length() > 0) {
				sb.append('.');
			}
			sb.append(id);
		}
		return sb.toString();
	}

}
