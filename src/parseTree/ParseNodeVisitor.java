package parseTree;

import parseTree.nodeTypes.ArrayExpressionNode;
import parseTree.nodeTypes.AssignmentNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BlockStatementNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BreakStatementNode;
import parseTree.nodeTypes.CallStatementNode;
import parseTree.nodeTypes.CastOperatorNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.CloneStatementNode;
import parseTree.nodeTypes.ContinueStatementNode;
import parseTree.nodeTypes.MainBlockNode;
import parseTree.nodeTypes.MapStatementNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ElseStatementNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.ExpressionListNode;
import parseTree.nodeTypes.FloatingConstantNode;
import parseTree.nodeTypes.FoldStatementNode;
import parseTree.nodeTypes.ForStatementIncrementNode;
import parseTree.nodeTypes.ForStatementNode;
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
import parseTree.nodeTypes.ReduceStatementNode;
import parseTree.nodeTypes.ReleaseStatementNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.ReverseStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.SubstringStatementNode;
import parseTree.nodeTypes.TabNode;
import parseTree.nodeTypes.TypeListNode;
import parseTree.nodeTypes.TypeLiteralNode;
import parseTree.nodeTypes.WhileStatementNode;
import parseTree.nodeTypes.ZipStatementNode;

// Visitor pattern with pre- and post-order visits
public interface ParseNodeVisitor {
	
	// non-leaf nodes: visitEnter and visitLeave
	void visitEnter(BinaryOperatorNode node);
	void visitLeave(BinaryOperatorNode node);
	
	void visitEnter(CastOperatorNode node);
	void visitLeave(CastOperatorNode node);
	
	void visitEnter(MainBlockNode node);
	void visitLeave(MainBlockNode node);
	
	void visitEnter(BlockStatementNode node);
	void visitLeave(BlockStatementNode node);

	void visitEnter(DeclarationNode node);
	void visitLeave(DeclarationNode node);
	
	void visitEnter(AssignmentNode node);
	void visitLeave(AssignmentNode node);

	void visitEnter(IfStatementNode node);
	void visitLeave(IfStatementNode node);
	
	void visitEnter(ElseStatementNode node);
	void visitLeave(ElseStatementNode node);
	
	void visitEnter(WhileStatementNode node);
	void visitLeave(WhileStatementNode node);
	
	void visitEnter(ForStatementNode node);
	void visitLeave(ForStatementNode node);
	
	void visitEnter(ForStatementIncrementNode node);
	void visitLeave(ForStatementIncrementNode node);
	
	void visitEnter(NotOperatorNode node);
	void visitLeave(NotOperatorNode node);
	
	void visitEnter(ReleaseStatementNode node);
	void visitLeave(ReleaseStatementNode node);
	
	void visitEnter(LengthStatementNode node);
	void visitLeave(LengthStatementNode node);
	
	void visitEnter(CloneStatementNode node);
	void visitLeave(CloneStatementNode node);
	
	void visitEnter(IndexOperatorNode node);
	void visitLeave(IndexOperatorNode node);
	
	void visitEnter(ParseNode node);
	void visitLeave(ParseNode node);
	
	void visitEnter(PrintStatementNode node);
	void visitLeave(PrintStatementNode node);
	
	void visitEnter(ProgramNode node);
	void visitLeave(ProgramNode node);
	
	void visitEnter(ArrayExpressionNode node);
	void visitLeave(ArrayExpressionNode node);
	
	void visitEnter(BreakStatementNode node);
	void visitLeave(BreakStatementNode node);

	void visitEnter(CallStatementNode node);
	void visitLeave(CallStatementNode node);

	void visitEnter(ContinueStatementNode node);
	void visitLeave(ContinueStatementNode node);

	void visitEnter(FunctionDefinitionNode node);
	void visitLeave(FunctionDefinitionNode node);

	void visitEnter(LambdaNode node);
	void visitLeave(LambdaNode node);

	void visitEnter(LambdaParamTypeNode node);
	void visitLeave(LambdaParamTypeNode node);

	void visitEnter(ParameterListNode node);
	void visitLeave(ParameterListNode node);
	
	void visitEnter(TypeListNode node);
	void visitLeave(TypeListNode node);

	void visitEnter(ParameterSpecificationNode node);
	void visitLeave(ParameterSpecificationNode node);

	void visitEnter(ReturnStatementNode node);
	void visitLeave(ReturnStatementNode node);
	
	void visitEnter(ExpressionListNode node);
	void visitLeave(ExpressionListNode node);

	void visitEnter(FunctionInvocationNode node);
	void visitLeave(FunctionInvocationNode node);
	
	void visitEnter(SubstringStatementNode node);
	void visitLeave(SubstringStatementNode node);
	
	void visitEnter(MapStatementNode node);
	void visitLeave(MapStatementNode node);
	
	void visitEnter(FoldStatementNode node);
	void visitLeave(FoldStatementNode node);
	
	void visitEnter(ReduceStatementNode node);
	void visitLeave(ReduceStatementNode node);
	
	void visitEnter(ZipStatementNode node);
	void visitLeave(ZipStatementNode node);
	
	void visitEnter(ReverseStatementNode node);
	void visitLeave(ReverseStatementNode node);

	// leaf nodes: visitLeaf only
	void visit(TypeLiteralNode node);
	void visit(BooleanConstantNode node);
	void visit(StringConstantNode node);
	void visit(CharacterConstantNode node);
	void visit(ErrorNode node);
	void visit(IdentifierNode node);
	void visit(IntegerConstantNode node);
	void visit(FloatingConstantNode node);
	void visit(NewlineNode node);
	void visit(TabNode node);
	void visit(SpaceNode node);

	
	public static class Default implements ParseNodeVisitor
	{
		public void defaultVisit(ParseNode node) {	}
		public void defaultVisitEnter(ParseNode node) {
			defaultVisit(node);
		}
		public void defaultVisitLeave(ParseNode node) {
			defaultVisit(node);
		}		
		public void defaultVisitForLeaf(ParseNode node) {
			defaultVisit(node);
		}
		
		public void visitEnter(BinaryOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(BinaryOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(CastOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(CastOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(DeclarationNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(DeclarationNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(AssignmentNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(AssignmentNode node) {
			defaultVisitLeave(node);
		}		
		public void visitEnter(MainBlockNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(MainBlockNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(BlockStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(BlockStatementNode node) {
			defaultVisitLeave(node);
		}	
		public void visitEnter(IfStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(IfStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ElseStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ElseStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(WhileStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(WhileStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ForStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ForStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ForStatementIncrementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ForStatementIncrementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(NotOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(NotOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ReleaseStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ReleaseStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(LengthStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(LengthStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(CloneStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(CloneStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(IndexOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(IndexOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ParseNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ParseNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(PrintStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(PrintStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ProgramNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ProgramNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ArrayExpressionNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ArrayExpressionNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(BreakStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(BreakStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(CallStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(CallStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ContinueStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ContinueStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(FunctionDefinitionNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(FunctionDefinitionNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(LambdaNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(LambdaNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(LambdaParamTypeNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(LambdaParamTypeNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ParameterListNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ParameterListNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(TypeListNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(TypeListNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ParameterSpecificationNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ParameterSpecificationNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ReturnStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ReturnStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ExpressionListNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ExpressionListNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(FunctionInvocationNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(FunctionInvocationNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(SubstringStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(SubstringStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(MapStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(MapStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(FoldStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(FoldStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ReduceStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ReduceStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ZipStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ZipStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ReverseStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ReverseStatementNode node) {
			defaultVisitLeave(node);
		}
		
		public void visit(TypeLiteralNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(BooleanConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(StringConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(CharacterConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(ErrorNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(IdentifierNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(IntegerConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(FloatingConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(NewlineNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(TabNode node) {
			defaultVisitForLeaf(node);
		}	
		public void visit(SpaceNode node) {
			defaultVisitForLeaf(node);
		}
	}

}
