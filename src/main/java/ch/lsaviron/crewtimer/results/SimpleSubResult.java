package ch.lsaviron.crewtimer.results;

import java.util.List;
import java.util.SortedMap;

record SimpleSubResult(
		SortedMap<EventCategoryKey, List<CategoryResult>> results)
		implements SubResult {

	@Override
	public void init() {
		// default: nothing to do
	}

	@Override
	public void end() {
		// default: nothing to do
	}

	@Override
	public String getName() {
		return "TOUS";
	}

	@Override
	public SortedMap<EventCategoryKey, List<CategoryResult>> getResults() {
		return results;
	}
}