package lexicalAnalyzer;

import java.util.HashSet;
import java.util.Set;

import inputHandler.LocatedChar;
import inputHandler.PushbackCharStream;
import logging.PikaLogger;
import tokens.FloatingToken;
import tokens.NumberToken;
import tokens.Token;

public class NumberScanner {
	private static final char DOT = '.';
	private static final char POSITIVE = '+';
	private static final char NEGATIVE = '-';
	private static final char EXPONENT = 'E';
	
	public static Token scanNumber(LocatedChar firstChar, PushbackCharStream input) {
		StringBuffer buffer = new StringBuffer();
		//if (firstChar.isChar(DOT)) {
		//	buffer.append('0');
		//}
		buffer.append(firstChar.getCharacter());
		LocatedChar c;
		c = input.next();
		if (!(c.isChar(DOT) || firstChar.isChar(DOT)) && c.isDigit()) {
			buffer.append(c.getCharacter());
			appendSubsequentDigits(buffer, input);
			c = input.next();
		}
		if ((firstChar.isChar(DOT) || c.isChar(DOT)) && (input.peek().isDigit() || c.isDigit())) {
			buffer.append(c.getCharacter());
			appendSubsequentDigits(buffer, input);
			c = input.next();
			if (c.getCharacter() == EXPONENT) {
				buffer.append(c.getCharacter());
				c = input.next();
				if ((c.getCharacter() == POSITIVE || c.getCharacter() == NEGATIVE) && input.peek().isDigit()) {
					buffer.append(c.getCharacter());
					appendSubsequentDigits(buffer, input);
					return FloatingToken.make(firstChar.getLocation(), buffer.toString());
				} else if (c.isDigit()) {
					buffer.append(c.getCharacter());
					appendSubsequentDigits(buffer, input);
					return FloatingToken.make(firstChar.getLocation(), buffer.toString());
				} else {
					lexicalError(c);
					return null;
				}
			} else {
				input.pushback(c);
				return FloatingToken.make(firstChar.getLocation(), buffer.toString());
			}
		} else {
			input.pushback(c);
			return NumberToken.make(firstChar.getLocation(), buffer.toString());
		}
	}
	public static void appendSubsequentDigits(StringBuffer buffer, PushbackCharStream input) {
		LocatedChar c = input.next();
		while(c.isDigit()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}
	
	public static boolean isNumberStart(LocatedChar ch, PushbackCharStream input) {
		if (ch.getCharacter() == POSITIVE || ch.getCharacter() == NEGATIVE) {
			if (input.peek().getCharacter() == DOT || input.peek().isDigit()) {
				return true;
			}
		} else if (ch.getCharacter() == DOT && input.peek().isDigit()) {
			return true;
		} else if (ch.isDigit()) {
			return true;
		}
		return false;
	}
	
	private static void lexicalError(LocatedChar ch) {
		PikaLogger log = PikaLogger.getLogger("compiler.numberScanner");
		log.severe("Lexical error: invalid character " + ch);
	}
}
