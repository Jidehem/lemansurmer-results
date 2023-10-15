package ch.lsaviron.crewtimer.results;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.ClientAnchor.AnchorType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Units;

/**
 * @author Jean-David Maillefer
 */
abstract class ExcelPrintHelper implements PrintHelper {

	private static final String DEFAULT_FONT_NAME = "Trebuchet MS";

	private static final double CM_PER_INCH = 2.54d;

	private final String outputFile;

	private final Workbook wb;

	private Sheet sheet;

	int rownum;

	private final CellStyle resultHeaderStyle;

	private final CellStyle raceHeaderStyle;

	private final CellStyle crewCellStyle;

	private EnumMap<Logo, Integer> logoIdx;

	private final Properties messages;

	enum Logo {

		LEMAN_SUR_MER("logo-lemansurmer.jpg"),
		SWISS_ROWING("logo-swissrowing.png");

		String filename;

		private Logo(final String filename) {
			this.filename = filename;
		}

		public int getPictureType() {
			if (filename.toLowerCase(Locale.ENGLISH).endsWith(".png")) {
				return Workbook.PICTURE_TYPE_PNG;
			}
			if (filename.toLowerCase(Locale.ENGLISH).endsWith(".jpg")) {
				return Workbook.PICTURE_TYPE_JPEG;
			}
			throw new RuntimeException("Unknown file type for " + filename);
		}
	}

	public ExcelPrintHelper(final String outputFile) {
		this.outputFile = outputFile;
		try {
			wb = createWorkbook();
		} catch (final IOException e) {
			throw new RuntimeException("Failed to created workbook", e);
		}

		initImages();

		final Font font0 = wb.createFont();
		font0.setFontName(DEFAULT_FONT_NAME);
		wb.getCellStyleAt(0).setFont(font0);

		final Font font = wb.createFont();
		font.setFontHeightInPoints((short) 13);
		font.setFontName(DEFAULT_FONT_NAME);
		font.setItalic(true);

		resultHeaderStyle = wb.createCellStyle();
		resultHeaderStyle.setFont(font);

		final Font font2 = wb.createFont();
		font2.setFontHeightInPoints((short) 18);
		font2.setFontName(DEFAULT_FONT_NAME);
		font2.setBold(true);

		raceHeaderStyle = wb.createCellStyle();
		raceHeaderStyle.setFont(font2);

		crewCellStyle = wb.createCellStyle();
		crewCellStyle.setFont(font0);
		crewCellStyle.setWrapText(true);

		messages = loadMessages();
	}

	private void initImages() {
		logoIdx = new EnumMap<>(Logo.class);
		for (final Logo logo : Logo.values()) {
			final byte[] bytes = loadImage(logo.filename);
			logoIdx.put(logo, wb.addPicture(bytes, logo.getPictureType()));
		}
	}

	abstract Workbook createWorkbook() throws IOException;

	public void initSheet(final String keyPrefix) {
		final String sheetName = getMessageSafe(
				"tab." + keyPrefix + ".tabName");
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

		// set margins
		sheet.setMargin(PageMargin.TOP, 0.5 / CM_PER_INCH/* inches */ );
		sheet.setMargin(PageMargin.RIGHT, 0.5 / CM_PER_INCH/* inches */ );
		sheet.setMargin(PageMargin.BOTTOM, 0.5 / CM_PER_INCH/* inches */ );
		sheet.setMargin(PageMargin.LEFT, 0.5 / CM_PER_INCH/* inches */ );

		//sheet.setDefaultRowHeight((short) 300);

		initHeader(keyPrefix);
	}

	public static int cmToEmu(final double distanceInCm) {
		return (int) (distanceInCm * Units.EMU_PER_CENTIMETER);
	}

	public void endSheet() {
		final CreationHelper helper = wb.getCreationHelper();
		// Create the drawing patriarch.  This is the top level container for all shapes.
		final Drawing<?> drawing = sheet.createDrawingPatriarch();
		{
			// LSM logo
			final ClientAnchor anchor = helper.createClientAnchor();
			anchor.setAnchorType(AnchorType.DONT_MOVE_AND_RESIZE);
			anchor.setCol1(0);
			anchor.setRow1(0);
			anchor.setCol2(2);
			anchor.setRow2(4);
			// coordinates must be in cell, else strange "Jumps" may occur
			anchor.setDx1(cmToEmu(.4));
			anchor.setDy1(cmToEmu(.4));
			// base size: 566x566
			anchor.setDx2(cmToEmu(.96));
			anchor.setDy2(cmToEmu(.2));
			drawing.createPicture(anchor, logoIdx.get(Logo.LEMAN_SUR_MER));
		}

		{
			final ClientAnchor anchor = helper.createClientAnchor();
			anchor.setAnchorType(AnchorType.DONT_MOVE_AND_RESIZE);
			anchor.setCol1(4);
			anchor.setRow1(0);
			anchor.setCol2(5);
			anchor.setRow2(4);
			// coordinates must be in cell, else strange "Jumps" may occur
			anchor.setDx1(cmToEmu(2.03));
			anchor.setDy1(cmToEmu(.4));
			// Swiss Rowing logo
			// base size: 348x390 (0.8923)
			anchor.setDx2(cmToEmu(2));
			anchor.setDy2(cmToEmu(.2));
			drawing.createPicture(anchor, logoIdx.get(Logo.SWISS_ROWING));
		}
		setPrintArea();
	}

	private void setPrintArea() {
		final PrintSetup printSetup = sheet.getPrintSetup();
		sheet.setFitToPage(true);
		printSetup.setFitWidth((short) 1);
		printSetup.setFitHeight((short) 0);
		printSetup.setLandscape(false);
		printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);

		sheet.setPrintGridlines(false);
		sheet.setAutobreaks(false);
		wb.setPrintArea(wb.getSheetIndex(sheet), 0, 5, 5, rownum - 1);
		// repeat first 5 rows on each page
		sheet.setRepeatingRows(CellRangeAddress.valueOf("1:5"));
	}

	private void initHeader(final String keyPrefix) {
		// TODO: this is too complex. Check if simpler to use a template instead (but need to have a template for both XLS and XLSX ?)
		Row row = sheet.createRow(rownum);
		Cell cell = row.createCell(0);
		CellStyle cellStyle = wb.createCellStyle();
		cellStyle.setAlignment(HorizontalAlignment.CENTER);
		Font font = createFontForHeader();
		font.setBold(true);
		font.setFontHeightInPoints((short) 22);
		cellStyle.setFont(font);
		cell.setCellStyle(cellStyle);
		cell.setCellValue(getMessageSafe("title.main"));
		sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 5));
		rownum++;

		final CellStyle cellStyleTemplate = cellStyle;

		row = sheet.createRow(rownum);
		cell = row.createCell(0);
		cellStyle = wb.createCellStyle();
		cellStyle.cloneStyleFrom(cellStyleTemplate);
		font = createFontForHeader();
		font.setBold(true);
		font.setFontHeightInPoints((short) 16);
		cellStyle.setFont(font);
		cell.setCellStyle(cellStyle);
		cell.setCellValue(getMessageSafe("title.detail"));
		sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 5));
		rownum++;

		row = sheet.createRow(rownum);
		cell = row.createCell(0);
		cellStyle = wb.createCellStyle();
		cellStyle.cloneStyleFrom(cellStyleTemplate);
		font = createFontForHeader();
		font.setFontHeightInPoints((short) 16);
		cellStyle.setFont(font);
		cell.setCellStyle(cellStyle);
		cell.setCellValue(getMessageSafe("title.date"));
		sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 5));
		rownum++;

		row = sheet.createRow(rownum);
		cell = row.createCell(0);
		cellStyle = wb.createCellStyle();
		cellStyle.cloneStyleFrom(cellStyleTemplate);
		font = createFontForHeader();
		font.setBold(true);
		font.setFontHeightInPoints((short) 13);
		cellStyle.setFont(font);
		cell.setCellStyle(cellStyle);
		cell.setCellValue(getMessageSafe("tab." + keyPrefix + ".title"));
		sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 5));
		rownum++;

		row = sheet.createRow(rownum);
		row.setHeightInPoints(15);
		rownum++;
	}

	private String getMessageSafe(final String key) {
		if (!messages.containsKey(key)) {
			throw new RuntimeException("No message found for key " + key);
		}
		return messages.getProperty(key);
	}

	private Font createFontForHeader() {
		final Font font = wb.createFont();
		font.setFontName(DEFAULT_FONT_NAME + " Bold Italic");
		return font;
	}

	private byte[] loadImage(final String image) {
		try {
			return IOUtils.toByteArray(ExcelPrintHelper.class.getClassLoader()
					.getResourceAsStream("images/" + image));
		} catch (final IOException e) {
			throw new RuntimeException("Failed to load image " + image, e);
		}
	}

	private Properties loadMessages() {
		final Properties res = new Properties();
		try (InputStream is = ExcelPrintHelper.class.getClassLoader()
				.getResourceAsStream("excel_messages.properties");
				InputStreamReader isr = new InputStreamReader(is,
						StandardCharsets.UTF_8)) {
			res.load(isr);
		} catch (final IOException e) {
			throw new RuntimeException("Failed to load Excel messages", e);
		}
		return res;
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
		row.createCell(col++)
				.setCellValue(PrintHelper.formatRank(categoryRank));
		row.createCell(col++).setCellValue(medals);
		row.createCell(col++).setCellValue(crewAbbrev);
		final Cell cell = row.createCell(col++);
		cell.setCellValue(crew);
		cell.setCellStyle(crewCellStyle);
		row.createCell(col++).setCellValue(PrintHelper.formatAdjTime(adjTime));
		row.createCell(col++).setCellValue(PrintHelper.formatDelta(delta));
		//System.out.printf("rank: %s crew: %s%n", categoryRank, crewAbbrev);
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
		return Arrays.asList(new ExcelSubResult("swissChampionship") {

			@Override
			public SortedMap<EventCategoryKey, List<CategoryResult>> getResults() {
				return results.entrySet().stream()
						.filter(e -> e.getKey().isSwissChampionshipCategory())
						.collect(getCollector(results));
			}

		}, new ExcelSubResult("lsm") {

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

		private final String keyPrefix;

		ExcelSubResult(final String keyPrefix) {
			this.keyPrefix = keyPrefix;
		}

		@Override
		public void init() {
			initSheet(keyPrefix);
		}

		@Override
		public void end() {
			endSheet();
		}

	}
}
