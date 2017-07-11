package tokens;

import inputHandler.TextLocation;

public class IndexOperatorToken extends TokenImp {
	static String operator = "[int]";
	protected IndexOperatorToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	
	public static IndexOperatorToken make(Token token) {
		IndexOperatorToken result = new IndexOperatorToken(token.getLocation(), operator);
		return result;
	}
	
	public static IndexOperatorToken make(TextLocation location, String lexeme) {
		IndexOperatorToken result = new IndexOperatorToken(location, lexeme);
		return result;
	}
	
	public static String getOperator() {
		return operator;
	}

	@Override
	protected String rawString() {
		return "index";
	}

}
