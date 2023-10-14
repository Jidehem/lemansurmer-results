package ch.lsaviron.swissrowing;

public enum SexCategory {

	// order by expected speed decreasing
	MEN("M"),
	MIXED("Mix"),
	WOMEN("W");

	final String code;

	SexCategory(final String code) {
		this.code = code;

	}
}
