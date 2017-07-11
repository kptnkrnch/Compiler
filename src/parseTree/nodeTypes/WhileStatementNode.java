package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class WhileStatementNode extends ParseNode {
	private String startLabel;
	private String endLabel;
	
	public WhileStatementNode(Token token) {
		super(token);
		startLabel = "";
		endLabel = "";
	}
	
	public WhileStatementNode(ParseNode node) {
		super(node);
		if (node instanceof WhileStatementNode) {
			this.startLabel = ((WhileStatementNode) node).startLabel;
			this.endLabel = ((WhileStatementNode) node).endLabel;
		}
	}
	
	////////////////////////////////////////////////////////////
	// no attributes
	
	public void setStartLabel(String label) {
		this.startLabel = label;
	}
	
	public String getStartLabel() { 
		return this.startLabel;
	}
	
	public void setEndLabel(String label) {
		this.endLabel = label;
	}
	
	public String getEndLabel() { 
		return this.endLabel;
	}
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
