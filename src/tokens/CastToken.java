package tokens;

import inputHandler.TextLocation;

public class CastToken extends TokenImp {
	static String operator = "[|]";
	protected CastToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	
	public static CastToken make(Token token) {
		CastToken result = new CastToken(token.getLocation(), operator);
		return result;
	}
	
	public static CastToken make(TextLocation location, String lexeme) {
		CastToken result = new CastToken(location, lexeme);
		return result;
	}
	
	public static String getOperator() {
		return operator;
	}

	@Override
	protected String rawString() {
		return "cast";
	}

}
