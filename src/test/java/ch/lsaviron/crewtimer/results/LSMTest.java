package ch.lsaviron.crewtimer.results;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ch.lsaviron.lsm.LsmEventCategory;

class LSMTest {

	@ParameterizedTest
	@CsvSource("2023, src/main/data/r12944.csv, src/test/data/r12944-SCREEN.txt")
	@CsvSource("2024, src/main/data/r13930.csv, src/test/data/r13930-SCREEN.txt")
	final void testMain(final int year, final String inputCsv,
			final String outputText) throws Exception {
		final PrintStream sysout = System.out;
		final PrintStream syserr = System.err;
		LSM.setCurrentYear(year);

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream out = new PrintStream(baos);) {
			System.setOut(out);
			System.setErr(out);

			LSM.main(inputCsv, "SCREEN");
			final byte[] output = baos.toByteArray();
			assertEquals(Files.readString(Path.of(outputText)),
					new String(output, StandardCharsets.UTF_8));
		} finally {
			System.setOut(sysout);
			System.setErr(syserr);
			LSM.resetCurrentYear();
		}
	}

	@Test
	final void testToStandardCategory() {
		for (final LsmEventCategory lsmEventCategory : LsmEventCategory.EVENT_CATEGORIES) {
			assertEquals(lsmEventCategory.toString().replace("*", ""),
					LSM.toStandardCategory(lsmEventCategory));
		}
	}

}
