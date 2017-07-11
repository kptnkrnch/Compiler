package parseTree.nodeTypes;

import java.util.LinkedList;
import java.util.List;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class ExpressionListNode extends ParseNode {
	public ExpressionListNode(Token token) {
		super(token);
	}
	
	public ExpressionListNode(ParseNode node) {
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
	
	public List<Type> getParameterTypes() {
		LinkedList<Type> typeList = new LinkedList<Type>();
		for (ParseNode child : this.getChildren()) {
			typeList.add(child.getType());
		}
		return typeList;
	}
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
