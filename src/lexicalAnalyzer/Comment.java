package lexicalAnalyzer;

import tokens.LextantToken;
import tokens.Token;

public enum Comment implements Lextant {
	COMMENT("#"),
	NULL_COMMENT("");

	private String lexeme;
	private Token prototype;
	
	private Comment(String lexeme) {
		this.lexeme = lexeme;
		this.prototype = LextantToken.make(null, lexeme, this);
	}
	
	public String getLexeme() {
		return lexeme;
	}
	public Token prototype() {
		return prototype;
	}
	
	public static Comment forLexeme(String lexeme) {
		for(Comment comment: values()) {
			if(comment.lexeme.equals(lexeme)) {
				return comment;
			}
		}
		return NULL_COMMENT;
	}
}
