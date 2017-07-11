package parseTree.nodeTypes;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class FunctionInvocationNode extends ParseNode {
	public FunctionInvocationNode(Token token) {
		super(token);
	}
	
	public FunctionInvocationNode(ParseNode node) {
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
	
	public static FunctionInvocationNode make(Token token, ParseNode identifier, ParseNode parameters) {
		FunctionInvocationNode node = new FunctionInvocationNode(token);
		node.appendChild(identifier);
		node.appendChild(parameters);
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
