package ch.lsaviron.swissrowing;

public enum AgeCategory {

	/** The default, standard age category. */
	SENIOR(""),
	U19("U19"),
	MASTER("M");

	final String prefix;

	AgeCategory(final String prefix) {
		this.prefix = prefix;
	}
}
