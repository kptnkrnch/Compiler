package semanticAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import asmCodeGenerator.IdentifierFactory;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import logging.PikaLogger;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.nodeTypes.ArrayExpressionNode;
import parseTree.nodeTypes.AssignmentNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BlockStatementNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BreakStatementNode;
import parseTree.nodeTypes.CastOperatorNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.CloneStatementNode;
import parseTree.nodeTypes.ContinueStatementNode;
import parseTree.nodeTypes.MainBlockNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.ExpressionListNode;
import parseTree.nodeTypes.FloatingConstantNode;
import parseTree.nodeTypes.FunctionDefinitionNode;
import parseTree.nodeTypes.FunctionInvocationNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IndexOperatorNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.LambdaNode;
import parseTree.nodeTypes.LambdaParamTypeNode;
import parseTree.nodeTypes.LengthStatementNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.NotOperatorNode;
import parseTree.nodeTypes.ParameterListNode;
import parseTree.nodeTypes.ParameterSpecificationNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReleaseStatementNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TabNode;
import parseTree.nodeTypes.TypeLiteralNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.TypePromoter;
import semanticAnalyzer.types.TypeVariable;
import semanticAnalyzer.types.VoidType;
import symbolTable.Binding;
import symbolTable.Scope;
import symbolTable.SymbolTable;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.Token;

class FunctionAnalysisVisitor extends ParseNodeVisitor.Default {
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// constructs larger than statements
	@Override
	public void visitEnter(ProgramNode node) {
		enterProgramScope(node);
	}
	public void visitLeave(ProgramNode node) {
		//leaveScope(node);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// helper methods for scoping.
	private void enterProgramScope(ParseNode node) {
		Scope scope = Scope.createProgramScope();
		node.setScope(scope);
	}	
	private void enterLambdaScope(ParseNode node) {
		Scope scope = Scope.createLambdaScope();
		node.setScope(scope);
	}
	//@SuppressWarnings("unused")
	private void enterSubscope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createSubscope();
		node.setScope(scope);
	}		
	private void leaveScope(ParseNode node) {
		node.getScope().leave();
	}
	
	public void visitLeave(FunctionDefinitionNode node) {
		assert node.nChildren() == 2;
		assert node.child(0) instanceof IdentifierNode;
		assert node.child(1) instanceof LambdaNode;
		
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		LambdaNode lambda = (LambdaNode) node.child(1);
		
		addBinding(identifier, lambda.getType(), false, false, true);
	}
	
	/*public void visitLeave(FunctionInvocationNode node) {
		assert node.nChildren() == 2;
		assert node.child(0) instanceof IdentifierNode;
		assert node.child(1) instanceof ExpressionListNode;
		
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ExpressionListNode parameters = (ExpressionListNode) node.child(1);
		Binding declaration = getDeclaration(identifier);
		List<Type> parameterTypes = parameters.getParameterTypes();
		if (declaration == null) {
			noPriorDeclarationError(identifier);
			node.setType(PrimitiveType.ERROR);
		}
		if (declaration != null && declaration.IsFunc() && declaration.getType() instanceof LambdaType) {
			LambdaType type = (LambdaType) declaration.getType();
			FunctionSignature signature = type.getSignature();
			if (signature.accepts(parameterTypes)) {
				node.setType(signature.resultType());
			} else {
				typeCheckError(node, parameterTypes);
				node.setType(PrimitiveType.ERROR);
			}
		} else {
			typeCheckError(node, parameterTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}*/
	
	public void visitEnter(LambdaNode node) {
		enterLambdaScope(node);
	}
	
	public void visitLeave(LambdaNode node) {
		//leaveScope(node);
		assert node.nChildren() == 2;
		assert node.child(0) instanceof LambdaParamTypeNode;
		assert node.child(1) instanceof BlockStatementNode;
		
		LambdaParamTypeNode paramTypes = (LambdaParamTypeNode)node.child(0);
		FunctionSignature signature = null;
		if (paramTypes.getType() instanceof LambdaType) {
			LambdaType lType = (LambdaType) paramTypes.getType();
			signature = lType.getSignature();
			node.setType(lType);
		}
	}
	
	public void visitLeave(LambdaParamTypeNode node) {
		assert node.nChildren() == 2;
		assert node.child(0) instanceof ParameterListNode;
		assert node.child(1) instanceof TypeLiteralNode;
		LambdaType type = new LambdaType();
		ParameterListNode parameterListNode = (ParameterListNode)node.child(0);
		TypeLiteralNode returnTypeNode = (TypeLiteralNode)node.child(1);
		List<Type> typeList = new LinkedList<Type>();
		Type returnType = returnTypeNode.getCompoundType();
		for (ParseNode temp : parameterListNode.getChildren()) {
			Type tempType = ((TypeLiteralNode)((ParameterSpecificationNode)temp).child(0)).getCompoundType();
			typeList.add(tempType);
		}
		typeList.add(returnType);
		FunctionSignature signature = new FunctionSignature(1, typeList);
		type.setSignature(signature);
		node.setType(type);
	}
	
	public void visitLeave(ParameterSpecificationNode node) {
		assert node.nChildren() == 2;
		assert node.child(0) instanceof TypeLiteralNode;
		assert node.child(1) instanceof IdentifierNode;
		
		TypeLiteralNode typeLiteral = (TypeLiteralNode)node.child(0);
		Type t = typeLiteral.getCompoundType();
		if (t instanceof VoidType || (t instanceof ArrayType && ((ArrayType)t).getRootType() instanceof VoidType)) {
			typeCheckError(node, Arrays.asList(t));
			node.setType(PrimitiveType.ERROR);
		} else {
			IdentifierNode identifier = (IdentifierNode)node.child(1);
			identifier.setType(t);
			node.setType(t);
			addBinding(identifier, t, true);
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	// simple leaf nodes
	@Override
	public void visit(BooleanConstantNode node) {
		node.setType(PrimitiveType.BOOLEAN);
	}
	@Override
	public void visit(ErrorNode node) {
		node.setType(PrimitiveType.ERROR);
	}
	@Override
	public void visit(IntegerConstantNode node) {
		node.setType(PrimitiveType.INTEGER);
	}
	@Override
	public void visit(FloatingConstantNode node) {
		node.setType(PrimitiveType.FLOAT);
	}
	@Override
	public void visit(StringConstantNode node) {
		node.setType(PrimitiveType.STRING);
	}
	@Override
	public void visit(CharacterConstantNode node) {
		node.setType(PrimitiveType.CHARACTER);
	}
	@Override
	public void visit(NewlineNode node) {
	}
	@Override
	public void visit(TabNode node) {
	}
	@Override
	public void visit(SpaceNode node) {
	}
	///////////////////////////////////////////////////////////////////////////
	// IdentifierNodes, with helper methods
	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return ((parent instanceof DeclarationNode) && (node == parent.child(0))) 
				|| (parent instanceof FunctionDefinitionNode)
				|| (parent instanceof ParameterSpecificationNode);
	}
	private void addBinding(IdentifierNode identifierNode, Type type, Boolean mutable) {
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type, mutable, false, false);
		identifierNode.setBinding(binding);
	}
	private Binding addGlobalBinding(IdentifierNode identifierNode, Type type, Boolean mutable, Boolean isStatic) {
		ParseNode parent = identifierNode;
		while (parent.getParent() != null) {
			parent = parent.getParent();
		}
		Scope scope = parent.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type, mutable, isStatic, false);
		identifierNode.setBinding(binding);
		return binding;
	}
	private Binding addStaticGlobalBinding(IdentifierNode identifierNode, Token newIdentifier, Type type, Boolean mutable) {
		ParseNode parent = identifierNode;
		while (parent.getParent() != null) {
			parent = parent.getParent();
		}
		Scope scope = parent.getScope();
		Binding binding = scope.createBinding(newIdentifier, type, type.getSize() + 1, mutable, true, false);
		identifierNode.setBinding(binding);
		return binding;
	}
	private void addBinding(IdentifierNode identifierNode, Type type, int bytesToAllocate, Boolean mutable, Boolean isStatic, Boolean isLambda) {
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type, bytesToAllocate, mutable, isStatic, isLambda);
		identifierNode.setBinding(binding);
	}
	private void addBinding(IdentifierNode identifierNode, Type type, Boolean mutable, Boolean isStatic, Boolean isLambda) {
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type, mutable, isStatic, isLambda);
		identifierNode.setBinding(binding);
	}
	private boolean wasDeclared(IdentifierNode identifierNode) {
		Scope scope = identifierNode.getDeclarationScope();
		SymbolTable declarations = scope.getSymbolTable();
		return declarations.containsKey(identifierNode.getToken().getLexeme());
	}
	private Binding getDeclaration(IdentifierNode identifierNode) {
		Scope scope = identifierNode.getDeclarationScope();
		SymbolTable declarations = scope.getSymbolTable();
		return declarations.lookup(identifierNode.getToken().getLexeme());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// error logging/printing

	private void typeCheckError(ParseNode node, List<Type> operandTypes) {
		Token token = node.getToken();
		
		logError("operator " + token.getLexeme() + " not defined for types " 
				 + operandTypes  + " at " + token.getLocation());	
	}
	private void noPriorDeclarationError(ParseNode node) {
		Token token = node.getToken();
		
		logError("identifier " + token.getLexeme() + " was not previously declared at " + token.getLocation());	
	}
	private void priorDeclarationIsImmutable(ParseNode node) {
		Token token = node.getToken();
		
		logError("identifier " + token.getLexeme() + " was not previously declared as being mutable at " + token.getLocation());
	}
	private void conflictingReturnTypeError(ParseNode node, Type expectedType, Type actualType) {
		Token token = node.getToken();
		logError("return type " + actualType + " does not match " + expectedType + " at " + token.getLocation());
	}
	private void couldNotFindControlFlowParentError(ParseNode currentNode) {
		Token token = currentNode.getToken();
		logError("could not find the control flow parent for the " + token.getLexeme() + " statement at " + token.getLocation());
	}
	private void logError(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
}