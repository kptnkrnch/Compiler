package parseTree.nodeTypes;

import java.util.LinkedList;
import java.util.List;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.VoidType;
import tokens.FloatingToken;
import tokens.Token;

public class TypeLiteralNode extends ParseNode {
	private TypeLiteralNode subNode;
	public TypeLiteralNode(Token token) {
		super(token);
	}
	public TypeLiteralNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	//attributes	
	
	public TypeLiteralNode getSubNode() {
		return subNode;
	}
	
	public void setSubNode(TypeLiteralNode node) {
		this.subNode = node;
	}
	
	public Type getCompoundType() {
		if (this.getToken().isLextant(Keyword.BOOL)) {
			return PrimitiveType.BOOLEAN;
		} else if (this.getToken().isLextant(Keyword.CHAR)) {
			return PrimitiveType.CHARACTER;
		} else if (this.getToken().isLextant(Keyword.FLOAT)) {
			return PrimitiveType.FLOAT;
		} else if (this.getToken().isLextant(Keyword.INT)) {
			return PrimitiveType.INTEGER;
		} else if (this.getToken().isLextant(Keyword.STRING)) {
			return PrimitiveType.STRING;
		} else if (this.getToken().isLextant(Keyword.RAT)) {
			return PrimitiveType.RATIONAL;
		} else if (this.getToken().isLextant(Keyword.VOID)) {
			return new VoidType();
		} else if (this.getToken().isLextant(Punctuator.OPEN_BRACKET)){
			Type t = new ArrayType(this.subNode.getCompoundType());
			return t;
		} else if (this.getToken().isLextant(Punctuator.LESSER)) {
			List<Type> typeList = new LinkedList<Type>();
			if (this.nChildren() == 2) {
				ParseNode parameterTypes = this.child(0);
				for (ParseNode temp : parameterTypes.getChildren()) {
					Type tempType = ((TypeLiteralNode)temp).getCompoundType();
					typeList.add(tempType);
				}
				TypeLiteralNode returnTypeNode = (TypeLiteralNode) this.child(1);
				Type returnType = returnTypeNode.getCompoundType();
				typeList.add(returnType);
			}
			FunctionSignature signature = new FunctionSignature(1, typeList);
			Type t = new LambdaType(signature);
			return t;
		} else {
			return PrimitiveType.ERROR;
		}
	}
	
	public Type getRootType() {
		if (this.subNode.getToken().isLextant(Keyword.BOOL)) {
			return PrimitiveType.BOOLEAN;
		} else if (this.subNode.getToken().isLextant(Keyword.CHAR)) {
			return PrimitiveType.CHARACTER;
		} else if (this.subNode.getToken().isLextant(Keyword.FLOAT)) {
			return PrimitiveType.FLOAT;
		} else if (this.subNode.getToken().isLextant(Keyword.INT)) {
			return PrimitiveType.INTEGER;
		} else if (this.subNode.getToken().isLextant(Keyword.STRING)) {
			return PrimitiveType.STRING;
		} else if (this.getToken().isLextant(Keyword.RAT)) {
			return PrimitiveType.RATIONAL;
		} else if (this.getToken().isLextant(Punctuator.OPEN_BRACKET)) {
			return this.subNode.getRootType();
		} else {
			return PrimitiveType.ERROR;
		}
	}
	
	///////////////////////////////////////////////////////////
	//accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
