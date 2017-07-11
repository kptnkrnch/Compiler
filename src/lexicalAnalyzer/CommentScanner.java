package lexicalAnalyzer;

import inputHandler.LocatedChar;
import inputHandler.PushbackCharStream;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.Token;

public class CommentScanner {
	public static boolean CommentDebugMode = false;
	public static final char START_COMMENT = '#';
	public static final char END_COMMENT = '#';
	public static final char END_COMMENT_NL = '\n';
	
	public static void scanComment(LocatedChar firstChar, PushbackCharStream input) {
		StringBuffer buffer = new StringBuffer();
	

		LocatedChar c = input.next();
		while(c.getCharacter() != END_COMMENT && c.getCharacter() != END_COMMENT_NL) {
			if (CommentDebugMode) {
				buffer.append(c.getCharacter());
			}
			c = input.next();
		}
		c = input.next();
		if (CommentDebugMode) {
			System.out.println("Comment: " + buffer.toString());
		}
		input.pushback(c);
	}
}
