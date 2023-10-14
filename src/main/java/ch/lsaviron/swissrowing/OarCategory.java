package ch.lsaviron.swissrowing;

public enum OarCategory {

	SINGLE_SCULL(""),
	DOUBLE_SCULL("x"),;

	final String code;

	OarCategory(final String code) {
		this.code = code;
	}
}
