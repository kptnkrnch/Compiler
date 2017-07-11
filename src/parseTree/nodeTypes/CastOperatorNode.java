package parseTree.nodeTypes;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class CastOperatorNode extends ParseNode {
	Type castType;
	
	public CastOperatorNode(Token token, Type type) {
		super(token);
		this.setType(type);
	}
	
	public CastOperatorNode(Token token) {
		super(token);
		this.setType(PrimitiveType.NO_TYPE);
	}

	public CastOperatorNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public String getOperator() {
		return token.getLexeme();
	}
	
	/*public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}*/	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static CastOperatorNode withChildren(Token token, ParseNode expression, Type type) {
		CastOperatorNode node = new CastOperatorNode(token, type);
		node.appendChild(expression);
		return node;
	}
	
	public static CastOperatorNode withChildren(Token token, ParseNode expression, ParseNode typeLiteral) {
		CastOperatorNode node = new CastOperatorNode(token, PrimitiveType.NO_TYPE);
		node.appendChild(expression);
		node.appendChild(typeLiteral);
		return node;
	}
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
