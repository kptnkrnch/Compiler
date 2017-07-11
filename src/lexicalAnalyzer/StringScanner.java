package lexicalAnalyzer;

import inputHandler.LocatedChar;
import inputHandler.PushbackCharStream;
import logging.PikaLogger;
import tokens.StringToken;
import tokens.Token;

public class StringScanner {
	public static boolean CommentDebugMode = false;
	public static final char START_STRING = '\"';
	public static final char END_STRING = '\"';
	public static final char ILLEGAL_CHARACTER_NL = '\n';
	
	public static Token scanString(LocatedChar firstChar, PushbackCharStream input) {
		StringBuffer buffer = new StringBuffer();
	

		LocatedChar c = input.next();
		while(c.getCharacter() != END_STRING) {
			if (c.isChar(ILLEGAL_CHARACTER_NL)) {
				lexicalError(c);
			}
			buffer.append(c.getCharacter());
			c = input.next();
		}
		return StringToken.make(firstChar.getLocation(), buffer.toString());
	}
	
	private static void lexicalError(LocatedChar ch) {
		PikaLogger log = PikaLogger.getLogger("compiler.CharacterScanner");
		log.severe("Lexical error: invalid character " + ch);
	}
}
