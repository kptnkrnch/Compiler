package parseTree.nodeTypes;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class LambdaParamTypeNode extends ParseNode {
	public LambdaParamTypeNode(Token token) {
		super(token);
	}
	
	public LambdaParamTypeNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// no attributes
	public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static LambdaParamTypeNode make(Token token, ParseNode parameterList, ParseNode returnType) {
		LambdaParamTypeNode node = new LambdaParamTypeNode(token);
		node.appendChild(parameterList);
		node.appendChild(returnType);
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
