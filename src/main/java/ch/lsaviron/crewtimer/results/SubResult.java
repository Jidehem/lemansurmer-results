package ch.lsaviron.crewtimer.results;

import java.util.List;
import java.util.SortedMap;

public interface SubResult {

	void init();

	void end();

	SortedMap<EventCategoryKey, List<CategoryResult>> getResults();

}
