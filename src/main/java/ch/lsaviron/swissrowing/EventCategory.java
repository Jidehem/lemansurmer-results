package ch.lsaviron.swissrowing;

import java.util.Comparator;

public record EventCategory(AgeCategory age, SexCategory sex,
		RowingCategory rowing, int rowers, OarCategory oar, boolean coxed) {

	public static final Comparator<EventCategory> BY_EXPECTED_SPEED_COMPARATOR = Comparator
			.comparing(EventCategory::rowers).reversed()
			.thenComparing(EventCategory::sex);

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(age.prefix);
		sb.append(sex.code);
		sb.append(' ');
		sb.append(rowing.code);
		sb.append(rowers);
		sb.append(oar.code);
		if (coxed) {
			sb.append('+');
		} else if (rowers == 8) {
			sb.append('-');
		}
		return sb.toString();
	}

	public static Comparator<EventCategory> EXPECTED_SPEED_COMPARATOR = Comparator
			.comparing(EventCategory::rowers).reversed()
			.thenComparing(EventCategory::sex);

	public static EventCategory parse(final String raw) {
		final int startIdx = 0;
		int endIdx = raw.length();

		// coxed
		boolean coxed;
		char coxwain = raw.charAt(endIdx - 1);
		if (coxwain == '+') {
			coxed = true;
			endIdx -= 1;
		} else if (coxwain == '-') {
			coxed = false;
			endIdx -= 1;
		} else {
			coxwain = ' ';
			coxed = false;
		}

		// oar
		OarCategory oar = null;
		// decreasing enum values search since the default, empty value comes first
		for (int i = OarCategory.values().length - 1; i >= 0; i--) {
			final OarCategory oc = OarCategory.values()[i];
			if (raw.substring(startIdx, endIdx).endsWith(oc.code)) {
				oar = oc;
				break;
			}
		}
		if (oar == null) {
			throw new IllegalArgumentException(
					"Unable to parse oar code for " + raw);
		}
		endIdx -= oar.code.length();

		// rowers
		final int rowers = raw.charAt(--endIdx) - '0';
		if (coxwain == ' ' && rowers == 8) {
			throw new IllegalArgumentException(
					"Coxwain must be specified for eights in " + raw);
		}

		// get rowing category + previous space
		RowingCategory rowing = null;
		// decreasing enum values search since the default, empty value comes first
		for (int i = RowingCategory.values().length - 1; i >= 0; i--) {
			final RowingCategory rc = RowingCategory.values()[i];
			if (raw.substring(startIdx, endIdx).endsWith(" " + rc.code)) {
				rowing = rc;
				break;
			}
		}
		if (rowing == null) {
			throw new IllegalArgumentException(
					"Unable to parse rowing category code for " + raw);
		}
		endIdx -= rowing.code.length() + 1;

		// sex
		SexCategory sex = null;
		for (final SexCategory sc : SexCategory.values()) {
			if (raw.substring(startIdx, endIdx).endsWith(sc.code)) {
				sex = sc;
				break;
			}
		}
		if (sex == null) {
			throw new IllegalArgumentException(
					"Unable to parse sex category code for " + raw);
		}
		endIdx -= sex.code.length();

		// age
		AgeCategory age = null;
		// decreasing enum values search since the default, empty value comes first
		for (int i = AgeCategory.values().length - 1; i >= 0; i--) {
			final AgeCategory ac = AgeCategory.values()[i];
			if (raw.substring(startIdx, endIdx).endsWith(ac.prefix)) {
				age = ac;
				break;
			}
		}
		if (age == null) {
			throw new IllegalArgumentException(
					"Unable to parse age category code for " + raw);
		}
		endIdx -= age.prefix.length();

		if (startIdx != endIdx) {
			throw new IllegalArgumentException(String.format(
					"Unable to parse fully %s: startIdx (%d) != endIdx (%d)",
					raw,
					startIdx,
					endIdx));
		}
		return new EventCategory(age, sex, rowing, rowers, oar, coxed);
	}

}
