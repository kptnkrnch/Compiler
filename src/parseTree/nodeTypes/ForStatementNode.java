package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ForStatementNode extends ParseNode {
	private String startLabel;
	private String endLabel;
	private String startIncrementLabel;
	private String endIncrementLabel;
	private boolean elemLoop;
	private boolean indexLoop;
	
	public ForStatementNode(Token token) {
		super(token);
		startLabel = "";
		endLabel = "";
		startIncrementLabel = "";
		endIncrementLabel = "";
		elemLoop = false;
		indexLoop = false;
	}
	
	public ForStatementNode(ParseNode node) {
		super(node);
		if (node instanceof ForStatementNode) {
			this.startLabel = ((ForStatementNode) node).startLabel;
			this.endLabel = ((ForStatementNode) node).endLabel;
			this.startIncrementLabel = ((ForStatementNode) node).startIncrementLabel;
			this.endIncrementLabel = ((ForStatementNode) node).endIncrementLabel;
			this.elemLoop = ((ForStatementNode) node).elemLoop;
			this.indexLoop = ((ForStatementNode) node).indexLoop;
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
	
	public void setStartIncrementLabel(String label) {
		this.startIncrementLabel = label;
	}
	
	public String getStartIncrementLabel() { 
		return this.startIncrementLabel;
	}
	
	public void setEndIncrementLabel(String label) {
		this.endIncrementLabel = label;
	}
	
	public String getEndIncrementLabel() { 
		return this.endIncrementLabel;
	}
	
	public void setIndexLoop(boolean indexLoop) {
		this.indexLoop = indexLoop;
	}
	
	public boolean getIndexLoop() { 
		return this.indexLoop;
	}
	
	public void setElemLoop(boolean elemLoop) {
		this.elemLoop = elemLoop;
	}
	
	public boolean getElemLoop() { 
		return this.elemLoop;
	}
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
