package parseTree.nodeTypes;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class LambdaNode extends ParseNode {
	private String startLabel;
	private String endLabel;
	
	public LambdaNode(Token token) {
		super(token);
		this.startLabel = "";
		this.endLabel = "";
	}
	
	public LambdaNode(ParseNode node) {
		super(node);
		if (node instanceof LambdaNode) {
			this.startLabel = ((LambdaNode) node).startLabel;
			this.endLabel = ((LambdaNode) node).endLabel;
		}
	}
	
	////////////////////////////////////////////////////////////
	// no attributes
	public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	public FunctionSignature getSignature() {
		LambdaParamTypeNode paramTypes = (LambdaParamTypeNode)this.child(0);
		FunctionSignature signature = null;
		if (paramTypes.getType() instanceof LambdaType) {
			LambdaType lType = (LambdaType) paramTypes.getType();
			signature = lType.getSignature();
			return signature;
		} else {
			return null;
		}
	}
	
	public Type getReturnType() {
		if (this.getSignature() != null) {
			return this.getSignature().resultType();
		} else {
			return PrimitiveType.ERROR;
		}
	}
	
	public String getStartLabel() {
		return this.startLabel;
	}
	public void setStartLabel(String label) {
		this.startLabel = label;
	}
	public String getEndLabel() {
		return this.endLabel;
	}
	public void setEndLabel(String label) {
		this.endLabel = label;
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static LambdaNode make(Token token, ParseNode lambdaParamType, ParseNode blockStatement) {
		LambdaNode node = new LambdaNode(token);
		node.appendChild(lambdaParamType);
		node.appendChild(blockStatement);
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
