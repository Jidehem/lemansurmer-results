package ch.lsaviron.crewtimer.results;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
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
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.util.IOUtils;

/**
 * @author Jean-David Maillefer
 */
abstract class ExcelPrintHelper implements PrintHelper {

	private static final String DEFAULT_FONT_NAME = "Trebuchet MS";

	private final String outputFile;

	private final Workbook wb;

	private Sheet sheet;

	int rownum;

	private final CellStyle resultHeaderStyle;

	private final CellStyle raceHeaderStyle;

	private final CellStyle crewCellStyle;

	private EnumMap<Logo, Integer> logoIdx;

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

	}

	private void initImages() {
		logoIdx = new EnumMap<>(Logo.class);
		for (final Logo logo : Logo.values()) {
			final byte[] bytes = loadImage(logo.filename);
			logoIdx.put(logo, wb.addPicture(bytes, logo.getPictureType()));
		}
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

		initHeader();
	}

	public void endSheet() {
		final CreationHelper helper = wb.getCreationHelper();
		// Create the drawing patriarch.  This is the top level container for all shapes.
		final Drawing<?> drawing = sheet.createDrawingPatriarch();
		// images must be after other sizing in the page to avoid some silly resizing effects
		{
			final ClientAnchor anchor = helper.createClientAnchor();
			anchor.setAnchorType(AnchorType.MOVE_DONT_RESIZE);
//			template pos: .36cm x; .15cm y
//			template size: 2.65L; 2.38H (A1)
			// set top-left corner of the picture,
//			anchor.setCol1(1);
//			anchor.setRow1(1);
//			anchor.setCol2(3);
//			anchor.setRow2(1);
			anchor.setDx1(200);
			anchor.setDy1(0);
			anchor.setDx2(1000);
			anchor.setDy2(1000);
			final Picture pict = drawing.createPicture(anchor,
					logoIdx.get(Logo.LEMAN_SUR_MER));
			pict.resize();
		}
		{
//			template pos: 2.69cm x; 0cm y
//			template size: 1.88L; 2.26H (B1)
			final ClientAnchor anchor = helper.createClientAnchor();
			anchor.setAnchorType(AnchorType.MOVE_DONT_RESIZE);
			// set top-left corner of the picture,
			anchor.setCol1(0);
			anchor.setRow1(0);
			//anchor.setDy1(900);
			final Picture pict = drawing.createPicture(anchor,
					logoIdx.get(Logo.SWISS_ROWING));
			pict.resize();
		}
	}

	private void initHeader() {

		Row row = sheet.createRow(rownum);
		Cell cell = row.createCell(0);
		CellStyle cellStyle = wb.createCellStyle();
		cellStyle.setAlignment(HorizontalAlignment.CENTER);
		Font font = createFontForHeader();
		font.setBold(true);
		font.setFontHeightInPoints((short) 22);
		cellStyle.setFont(font);
		cell.setCellStyle(cellStyle);
		cell.setCellValue("Léman-sur-mer");
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
		cell.setCellValue("Deuxième Championnats suisses d'aviron de mer");
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
		cell.setCellValue("Samedi 14 octobre 2023");
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
		// TODO set/adapt content based on sheet name/index
		cell.setCellValue("Résultats des Championnats suisses d'aviron de mer");
		//cell.setCellValue("Résultats par catégorie");
		sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 5));
		rownum++;

		// TODO make text configurable

	}

	private Font createFontForHeader() {
		final Font font = wb.createFont();
		font.setFontName(DEFAULT_FONT_NAME + " Bold Italic");
		return font;
	}

	private byte[] loadImage(final String image) {
		System.getProperty("java.class.path");
		try {
			return IOUtils.toByteArray(ExcelPrintHelper.class.getClassLoader()
					.getResourceAsStream("images/" + image));
		} catch (final IOException e) {
			throw new RuntimeException("Failed to load image " + image, e);
		}
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
			endSheet();
		}

		@Override
		public String getName() {
			return name;
		}

	}
}
