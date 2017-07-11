package parseTree.nodeTypes;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class ContinueStatementNode extends ParseNode {
	private ParseNode controlFlowParent;
	
	public ContinueStatementNode(Token token) {
		super(token);
		this.controlFlowParent = null;
	}
	
	public ContinueStatementNode(ParseNode node) {
		super(node);
		if (node instanceof ContinueStatementNode) {
			this.controlFlowParent = ((ContinueStatementNode)node).controlFlowParent;
		}
	}
	
	////////////////////////////////////////////////////////////
	// no attributes
	public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	public void setControlFlowParent(ParseNode parent) {
		this.controlFlowParent = parent;
	}
	
	public ParseNode getControlFlowParent() {
		return this.controlFlowParent;
	}
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
