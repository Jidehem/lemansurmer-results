package ch.lsaviron.swissrowing;

public enum AgeCategory {

	STANDARD(""),
	U19("U19"),
	MASTER("M");

	final String prefix;

	AgeCategory(final String prefix) {
		this.prefix = prefix;
	}
}
