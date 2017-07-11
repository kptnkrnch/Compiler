package parseTree.nodeTypes;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class BreakStatementNode extends ParseNode {
	private ParseNode controlFlowParent;
	
	public BreakStatementNode(Token token) {
		super(token);
		this.controlFlowParent = null;
	}
	
	public BreakStatementNode(ParseNode node) {
		super(node);
		if (node instanceof BreakStatementNode) {
			this.controlFlowParent = ((BreakStatementNode)node).controlFlowParent;
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
