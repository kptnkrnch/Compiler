package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class DeclarationNode extends ParseNode {
	private boolean global;
	public DeclarationNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.CONST) || token.isLextant(Keyword.VAR) || token.isLextant(Keyword.STATIC));
		this.global = false;
	}

	public DeclarationNode(ParseNode node) {
		super(node);
		if (node instanceof DeclarationNode) {
			this.global = ((DeclarationNode) node).global;
		}
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getDeclarationType() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}
	
	public void setGlobal(boolean isGlobal) {
		this.global = isGlobal;
	}
	
	public boolean isGlobal() {
		return this.global;
	}
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static DeclarationNode withChildren(Token token, ParseNode declaredName, ParseNode initializer) {
		DeclarationNode node = new DeclarationNode(token);
		node.appendChild(declaredName);
		node.appendChild(initializer);
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
