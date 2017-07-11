package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

/**
 * Name: MainBlockNode
 * Note: MainBlockNode is deprecated and has now been replaced
 *       by the more generic "BlockStatementNode".
 * @author Joshua
 *
 */
public class MainBlockNode extends ParseNode {

	public MainBlockNode(Token token) {
		super(token);
	}
	public MainBlockNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// no attributes

	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
