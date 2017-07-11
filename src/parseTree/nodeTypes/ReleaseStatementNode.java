package parseTree.nodeTypes;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class ReleaseStatementNode extends ParseNode {
	public ReleaseStatementNode(Token token) {
		super(token);
	}
	
	public ReleaseStatementNode(ParseNode node) {
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
	public static ReleaseStatementNode make(Token token, ParseNode expr) {
		ReleaseStatementNode node = new ReleaseStatementNode(token);
		node.appendChild(expr);
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
