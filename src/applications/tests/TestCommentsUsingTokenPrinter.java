package applications.tests;

import static applications.tests.FixtureDefinitions.COMMENTS_TOKEN_PRINTER_EXPECTED_FILENAME;
import static applications.tests.FixtureDefinitions.COMMENTS_TOKEN_PRINTER_INPUT_FILENAME;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import tests.FileFixturesTestCase;
import tokens.Tokens;
import applications.PikaTokenPrinter;

public class TestCommentsUsingTokenPrinter extends FileFixturesTestCase {
	public void testTokenPrinter() throws Exception {
		String actualOutput =	commentsTokenPrinterOutput(COMMENTS_TOKEN_PRINTER_INPUT_FILENAME);
		String expectedOutput = getContents(COMMENTS_TOKEN_PRINTER_EXPECTED_FILENAME);
		assertEquals(expectedOutput, actualOutput);
	}

	private String commentsTokenPrinterOutput(String filename) throws Exception {
		return outputFor(new CommentsTokenPrinterCommand(filename));
	}
	
	public class CommentsTokenPrinterCommand implements Command {
		String filename;
		public CommentsTokenPrinterCommand(String filename) {
			this.filename = filename;
		}

		public void run(PrintStream out) throws FileNotFoundException {
			Tokens.setPrintLevel(Tokens.Level.TYPE_AND_VALUE);
			PikaTokenPrinter.scanFile(filename, out);
		}
	}
}
