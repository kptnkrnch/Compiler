package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.PrimitiveType;
import tokens.FloatingToken;
import tokens.Token;

public class FloatingConstantNode extends ParseNode {
	public FloatingConstantNode(Token token) {
		super(token);
		assert(token instanceof FloatingToken);
		this.setType(PrimitiveType.FLOAT);
	}
	public FloatingConstantNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	//attributes
	
	public double getValue() {
		return floatingToken().getValue();
	}
	
	public FloatingToken floatingToken() {
		return (FloatingToken)token;
	}	
	
	///////////////////////////////////////////////////////////
	//accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}