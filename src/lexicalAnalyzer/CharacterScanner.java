package lexicalAnalyzer;

import inputHandler.LocatedChar;
import inputHandler.PushbackCharStream;
import logging.PikaLogger;
import tokens.CharacterToken;
import tokens.Token;

public class CharacterScanner {
	public static final char START_CHARACTER = '^';
	public static final char END_CHARACTER = '^';
	
	public static Token scanCharacter(LocatedChar firstChar, PushbackCharStream input) {
		StringBuffer buffer = new StringBuffer();
		LocatedChar c = input.next();
		buffer.append(c.getCharacter());
		c = input.next();
		
		if (!c.isChar(END_CHARACTER)) {
			String expected = "" + END_CHARACTER;
			lexicalError(c, expected);
		}
		return CharacterToken.make(firstChar.getLocation(), buffer.toString());
	}
	
	private static void lexicalError(LocatedChar ch, String expected) {
		PikaLogger log = PikaLogger.getLogger("compiler.CharacterScanner");
		String message = "Lexical error: invalid character " + ch;
		if (expected != null && expected.length() > 0) {
			message = message + " Expected: " + expected;
		}
		log.severe(message);
	}
}
