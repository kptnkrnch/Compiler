package tokens;

import inputHandler.TextLocation;

public class SubstringOperatorToken extends TokenImp {
	static String operator = "[int,int]";
	protected SubstringOperatorToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	
	public static SubstringOperatorToken make(Token token) {
		SubstringOperatorToken result = new SubstringOperatorToken(token.getLocation(), operator);
		return result;
	}
	
	public static SubstringOperatorToken make(TextLocation location, String lexeme) {
		SubstringOperatorToken result = new SubstringOperatorToken(location, lexeme);
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
