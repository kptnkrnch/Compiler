package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.PrimitiveType;
import tokens.CharacterToken;
import tokens.LextantToken;
import tokens.NumberToken;
import tokens.Token;

public class CharacterConstantNode extends ParseNode {
	public CharacterConstantNode(Token token) {
		super(token);
		assert(token instanceof CharacterToken);
		this.setType(PrimitiveType.CHARACTER);
	}
	public CharacterConstantNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes
	
	public char getValue() {
		return characterToken().getValue();
	}

	public CharacterToken characterToken() {
		return (CharacterToken)token;
	}	

///////////////////////////////////////////////////////////
// accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
