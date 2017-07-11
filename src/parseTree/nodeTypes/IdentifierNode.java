package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import logging.PikaLogger;
import symbolTable.Binding;
import symbolTable.MemoryAllocator;
import symbolTable.NegativeMemoryAllocator;
import symbolTable.Scope;
import tokens.IdentifierToken;
import tokens.Token;

public class IdentifierNode extends ParseNode {
	private Binding binding;
	private Scope declarationScope;
	
	boolean isMutable;
	boolean isStatic;

	public IdentifierNode(Token token) {
		super(token);
		assert(token instanceof IdentifierToken);
		this.binding = null;
		this.isMutable = false;
		this.isStatic = false;
	}
	public IdentifierNode(ParseNode node) {
		super(node);
		
		if(node instanceof IdentifierNode) {
			this.binding = ((IdentifierNode)node).binding;
			this.isStatic = ((IdentifierNode) node).isStatic;
			this.isMutable = ((IdentifierNode) node).isMutable;
		}
		else {
			this.binding = null;
			this.isMutable = false;
			this.isStatic = false;
		}
	}
	public IdentifierNode(Token token, ParseNode node) {
		super(node);
		assert(token instanceof IdentifierToken);
		this.token = token;
		if(node instanceof IdentifierNode) {
			this.binding = ((IdentifierNode)node).binding;
			this.isStatic = ((IdentifierNode) node).isStatic;
			this.isMutable = ((IdentifierNode) node).isMutable;
		}
		else {
			this.binding = null;
			this.isMutable = false;
			this.isStatic = false;
		}
	}
	
////////////////////////////////////////////////////////////
// attributes
	
	public IdentifierToken identifierToken() {
		return (IdentifierToken)token;
	}

	public void setBinding(Binding binding) {
		this.binding = binding;
	}
	public Binding getBinding() {
		return binding;
	}
	
	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}
	
	public void setMutable(boolean isMutable) {
		this.isMutable = isMutable;
	}
	
	public boolean isMutable() {
		return this.isMutable;
	}
	
	public boolean isStatic() {
		return this.isStatic;
	}
	
////////////////////////////////////////////////////////////
// Speciality functions

	public Binding findVariableBinding() {
		String identifier = token.getLexeme();

		for(ParseNode current : pathToRoot()) {
			if(current.containsBindingOf(identifier)) {
				declarationScope = current.getScope();
				return current.bindingOf(identifier);
			}
		}
		useBeforeDefineError();
		return Binding.nullInstance();
	}

	public Scope getDeclarationScope() {
		findVariableBinding();
		return declarationScope;
	}
	public void useBeforeDefineError() {
		PikaLogger log = PikaLogger.getLogger("compiler.semanticAnalyzer.identifierNode");
		Token token = getToken();
		log.severe("identifier " + token.getLexeme() + " used before defined at " + token.getLocation());
	}
	
	public boolean isNegativeAlloc() {
		MemoryAllocator temp = this.getLocalScope().getAllocationStrategy();
		return temp instanceof NegativeMemoryAllocator;
	}
	
///////////////////////////////////////////////////////////
// accept a visitor
		
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
