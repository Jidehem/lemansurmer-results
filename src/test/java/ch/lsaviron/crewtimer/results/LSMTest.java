package ch.lsaviron.crewtimer.results;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import ch.lsaviron.lsm.LsmEventCategory;

class LSMTest {

	@Test
	final void testMain() throws Exception {
		final PrintStream sysout = System.out;
		final PrintStream syserr = System.err;

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream out = new PrintStream(baos);) {
			System.setOut(out);
			System.setErr(out);

			LSM.main("src/main/data/r12944.csv", "SCREEN");
			final byte[] output = baos.toByteArray();
			assertEquals(
					Files.readString(
							Path.of("src/test/data/r12944-SCREEN.txt")),
					new String(output, StandardCharsets.UTF_8));
		} finally {
			System.setOut(sysout);
			System.setErr(syserr);
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
