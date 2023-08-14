package ch.lsaviron.crewtimer.results;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;

/**
 * @author Jean-David Maillefer
 */
abstract class ExcelPrintHelper implements PrintHelper {

	private final String outputFile;

	private final Workbook wb;

	private Sheet sheet;

	int rownum;

	private final CellStyle resultHeaderStyle;

	private final CellStyle raceHeaderStyle;

	private final CellStyle crewCellStyle;

	public ExcelPrintHelper(final String outputFile) {
		this.outputFile = outputFile;
		try {
			wb = createWorkbook();
		} catch (final IOException e) {
			throw new RuntimeException("Failed to created workbook", e);
		}
		final String fontName = "Trebuchet MS";
		final Font font0 = wb.createFont();
		font0.setFontName(fontName);
		wb.getCellStyleAt(0).setFont(font0);

		final Font font = wb.createFont();
		font.setFontHeightInPoints((short) 13);
		font.setFontName(fontName);
		font.setItalic(true);

		resultHeaderStyle = wb.createCellStyle();
		resultHeaderStyle.setFont(font);

		final Font font2 = wb.createFont();
		font2.setFontHeightInPoints((short) 18);
		font2.setFontName(fontName);
		font2.setBold(true);

		raceHeaderStyle = wb.createCellStyle();
		raceHeaderStyle.setFont(font2);

		crewCellStyle = wb.createCellStyle();
		crewCellStyle.setFont(font0);
		crewCellStyle.setWrapText(true);

	}

	abstract Workbook createWorkbook() throws IOException;

	public void initSheet(final String sheetName) {
		sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(sheetName));
		rownum = 0;

		int col = 0;
		sheet.setColumnWidth(col++, 1800);
		sheet.setColumnWidth(col++, 800);
		sheet.setColumnWidth(col++, 3300);
		sheet.setColumnWidth(col++, 18000);
		sheet.setColumnWidth(col++, 3000);
		sheet.setColumnWidth(col++, 3500);

		// print settings
		final PrintSetup ps = sheet.getPrintSetup();
		ps.setFitWidth((short) 1);

		//sheet.setDefaultRowHeight((short) 300);
	}

	@Override
	public void printRaceHeader(final String header) {
		final Row row1 = sheet.createRow(rownum);
		row1.setHeight((short) 1200);
		final Cell cellHeader = row1.createCell(0);
		cellHeader.setCellValue(header);
		cellHeader.setCellStyle(raceHeaderStyle);
		setMerged(rownum);
		++rownum;

		final Row row2 = sheet.createRow(rownum++);
		row2.setHeight((short) 700);
		// setting style on row does not seem to work
		//row2.setRowStyle(styleHeader);
		int colnum = 0;
		fill(row2.createCell(colnum++), "Rang", resultHeaderStyle);
		fill(row2.createCell(colnum++), "M", resultHeaderStyle);
		fill(row2.createCell(colnum++), "Nom court", resultHeaderStyle);
		fill(row2.createCell(colnum++), "Equipe", resultHeaderStyle);
		fill(row2.createCell(colnum++), "Temps", resultHeaderStyle);
		fill(row2.createCell(colnum++), "Différence", resultHeaderStyle);
	}

	private void setMerged(final int rownum) {
		sheet.addMergedRegion(new CellRangeAddress(rownum, //first row (0-based)
				rownum, //last row  (0-based)
				0, //first column (0-based)
				5 //last column  (0-based)
		));
	}

	private void fill(final Cell cell, final String value,
			final CellStyle style) {
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	@Override
	public void printResultRow(final Integer categoryRank, final String medals,
			final String crewAbbrev, final String crew, final String adjTime,
			final String delta) {
		final Row row = sheet.createRow(rownum++);
		// to allow auto-format for too long values
		row.setHeight((short) -1);
		int col = 0;
		row.createCell(col++).setCellValue(categoryRank);
		row.createCell(col++).setCellValue(medals);
		row.createCell(col++).setCellValue(crewAbbrev);
		final Cell cell = row.createCell(col++);
		cell.setCellValue(crew);
		cell.setCellStyle(crewCellStyle);
		row.createCell(col++).setCellValue(adjTime);
		row.createCell(col++).setCellValue(PrintHelper.formatDelta(delta));
	}

	@Override
	public void printRaceFooter() {
		// nothing to do
	}

	@Override
	public void end() {
		try (OutputStream fileOut = new FileOutputStream(outputFile)) {
			wb.write(fileOut);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<SubResult> getSubResults(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results) {
		return Arrays.asList(new ExcelSubResult("TOUS") {

			@Override
			public SortedMap<EventCategoryKey, List<CategoryResult>> getResults() {
				return results;
			}

		}, new ExcelSubResult("Championnat Suisse") {

			@Override
			public SortedMap<EventCategoryKey, List<CategoryResult>> getResults() {
				return results.entrySet().stream()
						.filter(e -> e.getKey().isSwissChampionshipCategory())
						.collect(getCollector(results));
			}

		}, new ExcelSubResult("LSM hors championnat Suisse") {

			@Override
			public SortedMap<EventCategoryKey, List<CategoryResult>> getResults() {
				return results.entrySet().stream()
						.filter(e -> !e.getKey().isSwissChampionshipCategory())
						.collect(getCollector(results));
			}

		});
	}

	private Collector<Entry<EventCategoryKey, List<CategoryResult>>, ?, TreeMap<EventCategoryKey, List<CategoryResult>>> getCollector(
			final SortedMap<EventCategoryKey, List<CategoryResult>> results) {
		return Collectors.toMap(
				Entry<EventCategoryKey, List<CategoryResult>>::getKey,
				Entry<EventCategoryKey, List<CategoryResult>>::getValue,
				(a, b) -> {
					throw new IllegalArgumentException(String.format(
							"Should not happen since already unique a: %s, b: %s"));
				},
				() -> new TreeMap<EventCategoryKey, List<CategoryResult>>(
						results.comparator()));
	}

	abstract class ExcelSubResult implements SubResult {

		private final String name;

		ExcelSubResult(final String name) {
			this.name = name;
		}

		@Override
		public void init() {
			initSheet(name);
		}

		@Override
		public void end() {
			// default: nothing to do
		}

		@Override
		public String getName() {
			return name;
		}

	}
}