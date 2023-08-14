package ch.lsaviron.crewtimer.results;

import java.util.List;
import java.util.SortedMap;

public interface SubResult {

	void init();

	void end();

	String getName();

	SortedMap<EventCategoryKey, List<CategoryResult>> getResults();

}
