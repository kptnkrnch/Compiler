package parseTree.nodeTypes;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class FunctionDefinitionNode extends ParseNode {
	public FunctionDefinitionNode(Token token) {
		super(token);
	}
	
	public FunctionDefinitionNode(ParseNode node) {
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
	
	public static FunctionDefinitionNode make(Token token, ParseNode identifier, ParseNode lambda) {
		FunctionDefinitionNode node = new FunctionDefinitionNode(token);
		node.appendChild(identifier);
		node.appendChild(lambda);
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
