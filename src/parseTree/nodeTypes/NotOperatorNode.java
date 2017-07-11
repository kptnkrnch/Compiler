package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import tokens.LextantToken;
import tokens.Token;

public class NotOperatorNode extends ParseNode {

	public NotOperatorNode(Token token) {
		super(token);
		assert(token.isLextant(Punctuator.NOT));
	}

	public NotOperatorNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}		
		
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static NotOperatorNode withChildren(Token token, ParseNode expression) {
		NotOperatorNode node = new NotOperatorNode(token);
		node.appendChild(expression);
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
