package ch.lsaviron.crewtimer.results;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * @author Jean-David Maillefer
 */
enum PrintMode {

	/**
	 * For printing in console (for developer).
	 */
	SCREEN {

		static PrintHelper SCREEN_PRINT_HELPER = new PrintHelper() {

			@Override
			public void printRaceHeader(final String header) {
				System.out.println();
				System.out.println(header);
				System.out.println("--------------------------");
			}

			@Override
			public void printResultRow(final Integer categoryRank,
					final String medals, final String crewAbbrev,
					final String crew, final String adjTime,
					final String delta) {

				System.out.printf("%d: %s\t%s\t%s\t%s\t%s%n",
						categoryRank,
						medals,
						crewAbbrev,
						crew,
						PrintHelper.formatAdjTime(adjTime),
						PrintHelper.formatDelta(delta));
			}

			@Override
			public void printRaceFooter() {
				System.out.println();
			}

			@Override
			public List<SubResult> getSubResults(
					final SortedMap<EventCategoryKey, List<CategoryResult>> results) {
				return Collections
						.<SubResult>singletonList(new SimpleSubResult(results));
			}
		};

		@Override
		PrintHelper buildHelper(final LSM lsm) {
			return SCREEN_PRINT_HELPER;
		}

	},
	/**
	 * For import in Excel or Google Sheet (using paste special > values
	 * only)
	 */
	TSV {

		static PrintHelper TSV_PRINT_HELPER = new PrintHelper() {

			@Override
			public void printRaceHeader(final String header) {
				System.out.println(header);
				System.out.printf(
						"Rang\tM\tNom court\tEquipe\tTemps\tDifférence%n");
			}

			@Override
			public void printResultRow(final Integer categoryRank,
					final String medals, final String crewAbbrev,
					final String crew, final String adjTime,
					final String delta) {

				System.out.printf("%d\t%s\t%s\t%s\t%s\t'%s%n",
						categoryRank,
						medals,
						crewAbbrev,
						crew,
						PrintHelper.formatAdjTime(adjTime),
						PrintHelper.formatDelta(delta));
			}

			@Override
			public void printRaceFooter() {
				System.out.println();
			}

			@Override
			public List<SubResult> getSubResults(
					final SortedMap<EventCategoryKey, List<CategoryResult>> results) {
				return Collections
						.<SubResult>singletonList(new SimpleSubResult(results));
			}
		};

		@Override
		PrintHelper buildHelper(final LSM lsm) {
			return TSV_PRINT_HELPER;
		}

	},
	XLS {

		@Override
		PrintHelper buildHelper(final LSM lsm) {
			return new ExcelPrintHelper(
					buildOutputFile(lsm, ".xls").toString()) {

				@Override
				Workbook createWorkbook() throws IOException {
					return WorkbookFactory.create(false);
				}
			};
		}

	},
	XLSX {

		@Override
		PrintHelper buildHelper(final LSM lsm) {
			return new ExcelPrintHelper(
					buildOutputFile(lsm, ".xlsx").toString()) {

				@Override
				Workbook createWorkbook() throws IOException {
					return WorkbookFactory.create(true);
				}
			};
		}

	};

	abstract PrintHelper buildHelper(LSM lsm);

	private static Path buildOutputFile(final LSM lsm, final String extension) {
		final Path in = Paths.get(lsm.resultsFromCrewTimerCsv);
		return in.resolveSibling(in.getFileName().toString()
				.replaceAll("(?i)\\.csv$", extension));
	}
}