package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.PrimitiveType;
import tokens.NumberToken;
import tokens.Token;

public class IntegerConstantNode extends ParseNode {
	public IntegerConstantNode(Token token) {
		super(token);
		assert(token instanceof NumberToken);
		this.setType(PrimitiveType.INTEGER);
	}
	public IntegerConstantNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes
	
	public int getValue() {
		return numberToken().getValue();
	}

	public NumberToken numberToken() {
		return (NumberToken)token;
	}	

///////////////////////////////////////////////////////////
// accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}

}
