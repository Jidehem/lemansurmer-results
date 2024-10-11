package ch.lsaviron.crewtimer.results;

/**
 * List all the result enum fields as return in CrewTimer results CSV. The name
 * must exactly match the one found in the first row (header).
 */
// Cf. ch.lsaviron.crewtimer.results.LSM#processResults()
enum CsvResultHeaders {
	EventNum,
	Event,
	Place,
	Crew,
	CrewAbbrev,
	Bow,
	Stroke,
	Start,
	Finish,
	// TODO nice-to-have allow adding/removing intermediate times dynamically ?
	// 2023 only
	//Bouée_A,
	//Bouée_C,
	RawTime,
	PenaltyCode,
	AdjTime,
	Delta
}
