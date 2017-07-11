package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import tokens.Token;

public class BlockStatementNode extends ParseNode {
	public BlockStatementNode(Token token) {
		super(token);
		this.returnType = null;
	}
	
	public BlockStatementNode(ParseNode node) {
		super(node);
		if (node instanceof BlockStatementNode) {
			this.returnType = ((BlockStatementNode) node).returnType;
		}
	}
	
	////////////////////////////////////////////////////////////
	// no attributes
	private Type returnType;
	
	public void setReturnType(Type type) {
		this.returnType = type;
	}
	
	public Type getReturnType() {
		return this.returnType;
	}
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
