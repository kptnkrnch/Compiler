package semanticAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import asmCodeGenerator.IdentifierFactory;
import asmCodeGenerator.Labeller;
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
import parseTree.nodeTypes.CallStatementNode;
import parseTree.nodeTypes.CastOperatorNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.CloneStatementNode;
import parseTree.nodeTypes.ContinueStatementNode;
import parseTree.nodeTypes.MainBlockNode;
import parseTree.nodeTypes.MapStatementNode;
import parseTree.nodeTypes.DeclarationNode;
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
import parseTree.nodeTypes.TypeLiteralNode;
import parseTree.nodeTypes.WhileStatementNode;
import parseTree.nodeTypes.ZipStatementNode;
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

class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// constructs larger than statements
	@Override
	public void visitEnter(ProgramNode node) {
		//enterProgramScope(node);
	}
	public void visitLeave(ProgramNode node) {
		leaveScope(node);
	}
	//public void visitEnter(MainBlockNode node) {
	//}
	//public void visitLeave(MainBlockNode node) {
	//}
	public void visitEnter(BlockStatementNode node) {
		enterSubscope(node);
	}
	public void visitLeave(BlockStatementNode node) {
		leaveScope(node);
		for (ParseNode child : node.getChildren()) {
			Type returnType = null;
			if (child instanceof BlockStatementNode) {
				returnType = ((BlockStatementNode) child).getReturnType();
			} else if (child instanceof ReturnStatementNode) {
				returnType = child.getType();
			}
			if (returnType != null && node.getReturnType() == null) {
				node.setReturnType(returnType);
			} else if (returnType != null && !node.getReturnType().match(returnType)) {
				conflictingReturnTypeError(child, node.getReturnType(), returnType);
				node.setType(PrimitiveType.ERROR);
				node.setReturnType(PrimitiveType.ERROR);
			}
		}
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
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// statements and declarations
	@Override
	public void visitLeave(PrintStatementNode node) {
	}
	@Override
	public void visitLeave(DeclarationNode node) {
		if (!node.isGlobal()) {
			IdentifierNode identifier = (IdentifierNode) node.child(0);
			ParseNode initializer = node.child(1);
			
			Type declarationType = initializer.getType();
	
			node.setType(declarationType);
			identifier.setTargetable(true);
			identifier.setType(declarationType);
			if (identifier.isStatic()) {
				String staticName = IdentifierFactory.makeIdentifier(identifier.getToken().getLexeme());
				IdentifierToken token = IdentifierToken.make(identifier.getToken().getLocation(), staticName);
				Binding binding = addStaticGlobalBinding(identifier, token, declarationType, identifier.isMutable());
				addBinding(identifier, declarationType, 0, identifier.isMutable(), identifier.isStatic(), false);
				identifier.getBinding().setMemoryLocation(binding.getMemoryLocation());
			} else {
				addBinding(identifier, declarationType, identifier.isMutable());
			}
		}
	}
	
	@Override
	public void visitLeave(AssignmentNode node) {
		assert node.nChildren() == 2;
		ParseNode identifier;
		Type identifierType;
		if (node.child(0) instanceof IdentifierNode) {
			//identifier = (IdentifierNode) node.child(0);
			identifier = node.child(0);
			identifierType = identifier.getType();
		} else if (node.child(0) instanceof IndexOperatorNode) {
			IndexOperatorNode temp = (IndexOperatorNode)node.child(0);
			//identifier = (IdentifierNode)temp.child(0);
			identifier = temp.child(0);
			identifierType = temp.getType();
			identifier.setTargetable(temp.getTargetable());
		} else {
			identifier = null;
			identifierType = null;
		}
		if (identifier != null) {
			ParseNode initializer = node.child(1);
			List<Type> childTypes = Arrays.asList(identifierType, initializer.getType());
			
			Lextant operator = operatorFor(node);
			//FunctionSignature signature = FunctionSignature.signatureOf(operator, childTypes);
			FunctionSignature signature = FunctionSignatures.signature(operator, childTypes);
			
			if (identifier instanceof IdentifierNode && node.child(0).getTargetable()) {
				if (wasDeclared((IdentifierNode)identifier)) {
					Binding declaration = getDeclaration((IdentifierNode)identifier);
					((IdentifierNode)identifier).setStatic(declaration.getStatic());
					if (declaration.getMutable() == true || (node.child(0) instanceof IndexOperatorNode)) {
						if (signature.accepts(childTypes)) {
							if (signature.resultType() instanceof TypeVariable) {
								node.setType(((TypeVariable)signature.resultType()).getType());
							} else {
								node.setType(signature.resultType());
							}
							node.setSignature(signature);
						} else {
							List<Type> promotedTypes = TypePromoter.findPromotions(operator, childTypes);
							if (promotedTypes != null) {
								signature = FunctionSignatures.signature(operator, promotedTypes);
								if(signature.accepts(promotedTypes)) {
									for (int typeNumber = 1; typeNumber < promotedTypes.size(); typeNumber++) {
										if (promotedTypes.get(typeNumber) != childTypes.get(typeNumber)) {
											ParseNode castNode = TypePromoter.promoteType(node.child(typeNumber), promotedTypes.get(typeNumber));
											node.replaceChild(node.child(typeNumber), castNode);
										}
									}
									if (signature.resultType() instanceof TypeVariable) {
										node.setType(((TypeVariable)signature.resultType()).getType());
									} else {
										node.setType(signature.resultType());
									}
									node.setSignature(signature);
								} else {
									typeCheckError(node, childTypes);
									node.setType(PrimitiveType.ERROR);
								}
							} else {
								typeCheckError(node, childTypes);
								node.setType(PrimitiveType.ERROR);
							}
						}
					} else {
						priorDeclarationIsImmutable(identifier);
					}
				} else {
					noPriorDeclarationError(identifier);
				}
			} else if (identifier.getTargetable()) {
				if (signature.accepts(childTypes)) {
					if (signature.resultType() instanceof TypeVariable) {
						node.setType(((TypeVariable)signature.resultType()).getType());
					} else {
						node.setType(signature.resultType());
					}
					node.setSignature(signature);
				} else {
					List<Type> promotedTypes = TypePromoter.findPromotions(operator, childTypes);
					if (promotedTypes != null) {
						signature = FunctionSignatures.signature(operator, promotedTypes);
						if(signature.accepts(promotedTypes)) {
							for (int typeNumber = 1; typeNumber < promotedTypes.size(); typeNumber++) {
								if (promotedTypes.get(typeNumber) != childTypes.get(typeNumber)) {
									ParseNode castNode = TypePromoter.promoteType(node.child(typeNumber), promotedTypes.get(typeNumber));
									node.replaceChild(node.child(typeNumber), castNode);
								}
							}
							if (signature.resultType() instanceof TypeVariable) {
								node.setType(((TypeVariable)signature.resultType()).getType());
							} else {
								node.setType(signature.resultType());
							}
							node.setSignature(signature);
						} else {
							typeCheckError(node, childTypes);
							node.setType(PrimitiveType.ERROR);
						}
					} else {
						typeCheckError(node, childTypes);
						node.setType(PrimitiveType.ERROR);
					}
				}
			} else {
				node.setType(PrimitiveType.ERROR);
				expressionIsNotTargetableError(identifier);
			}
		}
	}
	private Lextant operatorFor(AssignmentNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}
	
	@Override
	public void visitLeave(IfStatementNode node) {
		assert node.nChildren() >= 2;
		if (node.child(0) instanceof BooleanConstantNode) {
			BooleanConstantNode conditional = (BooleanConstantNode)node.child(0);
			List<Type> childTypes = Arrays.asList(conditional.getType());
			FunctionSignature signature = FunctionSignatures.signature(Keyword.IF, childTypes);
			
			if (!signature.accepts(childTypes)) {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			} else {
				node.setSignature(signature);
			}
		} else if (node.child(0) instanceof BinaryOperatorNode) {
			BinaryOperatorNode conditional = (BinaryOperatorNode)node.child(0);
			List<Type> childTypes = Arrays.asList(conditional.getType());
			FunctionSignature signature = FunctionSignatures.signature(Keyword.IF, childTypes);
			
			if (!signature.accepts(childTypes)) {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			} else {
				node.setSignature(signature);
			}
		} else {
			ParseNode conditional = node.child(0);
			List<Type> childTypes = Arrays.asList(conditional.getType());
			FunctionSignature signature = FunctionSignatures.signature(Keyword.IF, childTypes);
			
			if (!signature.accepts(childTypes)) {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			} else {
				node.setSignature(signature);
			}
		}
	}
	
	@Override
	public void visitLeave(WhileStatementNode node) {
		assert node.nChildren() == 2;
		if (node.child(0) instanceof BooleanConstantNode) {
			BooleanConstantNode conditional = (BooleanConstantNode)node.child(0);
			List<Type> childTypes = Arrays.asList(conditional.getType());
			FunctionSignature signature = FunctionSignatures.signature(Keyword.WHILE, childTypes);
			
			if (!signature.accepts(childTypes)) {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			} else {
				node.setSignature(signature);
			}
		} else if (node.child(0) instanceof BinaryOperatorNode) {
			BinaryOperatorNode conditional = (BinaryOperatorNode)node.child(0);
			List<Type> childTypes = Arrays.asList(conditional.getType());
			FunctionSignature signature = FunctionSignatures.signature(Keyword.WHILE, childTypes);
			
			if (!signature.accepts(childTypes)) {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			} else {
				node.setSignature(signature);
			}
		} else {
			ParseNode conditional = node.child(0);
			List<Type> childTypes = Arrays.asList(conditional.getType());
			FunctionSignature signature = FunctionSignatures.signature(Keyword.WHILE, childTypes);
			
			if (!signature.accepts(childTypes)) {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			} else {
				node.setSignature(signature);
			}
		}
	}
	
	@Override
	public void visitLeave(ForStatementIncrementNode node) {
		assert node.nChildren() == 3;
		assert node.child(0) instanceof IdentifierNode;
		IdentifierNode identifier = (IdentifierNode)node.child(0);
		ParseNode expression = node.child(1);
		IdentifierNode index = (IdentifierNode)node.child(2);
		index.setType(PrimitiveType.INTEGER);
		addBinding(index, index.getType(), true);
		
		Labeller labeller = new Labeller("for-loop-increment");
		String start = labeller.newLabel("start");
		String end = labeller.newLabel("end");
		node.setStartIncrementLabel(start);
		node.setEndIncrementLabel(end);
		
		if (expression instanceof IdentifierNode) {
			Binding binding = ((IdentifierNode)expression).findVariableBinding();
			((IdentifierNode)expression).setType(binding.getType());
			((IdentifierNode)expression).setBinding(binding);
		}
		
		if (node.getIndexLoop()) {
			identifier.setType(PrimitiveType.INTEGER);
			if (!(expression.getType() instanceof ArrayType) && 
					!(expression.getType() instanceof PrimitiveType && expression.getType() == PrimitiveType.STRING)) {
				identifier.setType(PrimitiveType.ERROR);
				node.setType(PrimitiveType.ERROR);
				IndexableTypeCheckError(expression);
			}
		} else if (node.getElemLoop()) {
			if (expression.getType() instanceof ArrayType) {
				identifier.setType(((ArrayType)expression.getType()).getSubType());
			} else if (expression.getType() instanceof PrimitiveType && expression.getType() == PrimitiveType.STRING) {
				identifier.setType(PrimitiveType.CHARACTER);
			} else {
				identifier.setType(PrimitiveType.ERROR);
				node.setType(PrimitiveType.ERROR);
				IndexableTypeCheckError(expression);
			}
		} else {
			identifier.setType(PrimitiveType.ERROR);
			node.setType(PrimitiveType.ERROR);
			ForLoopError(node);
		}
		
		addBinding(identifier, identifier.getType(), false);
	}
	
	@Override
	public void visitLeave(ForStatementNode node) {
		assert node.nChildren() == 2;
		assert node.child(0) instanceof ForStatementIncrementNode;
		ForStatementIncrementNode header = (ForStatementIncrementNode)node.child(0);
		Labeller labeller = new Labeller("forstatement");
		String startLabel = labeller.newLabel("start");
		String endLabel = labeller.newLabel("end");
		node.setStartLabel(startLabel);
		node.setEndLabel(endLabel);
		node.setStartIncrementLabel(header.getStartIncrementLabel());
		node.setEndIncrementLabel(header.getEndIncrementLabel());
		
		header.setStartLabel(startLabel);
		header.setEndLabel(endLabel);
	}
	
	@Override
	public void visitLeave(LengthStatementNode node) {
		assert node.nChildren() == 1;
		ParseNode child  = node.child(0);
		List<Type> childTypes = Arrays.asList(child.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
		
		if(signature.accepts(childTypes)) {
			if (signature.resultType() instanceof TypeVariable) {
				node.setType(((TypeVariable)signature.resultType()).getType());
			} else {
				node.setType(signature.resultType());
			}
			List<Type> signatureTypes = Arrays.asList(child.getType(), signature.resultType());
			signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
			node.setSignature(signature);
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	
	@Override
	public void visitLeave(ReverseStatementNode node) {
		assert node.nChildren() == 1;
		ParseNode child  = node.child(0);
		List<Type> childTypes = Arrays.asList(child.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
		
		if(signature.accepts(childTypes)) {
			if (signature.resultType() instanceof TypeVariable) {
				node.setType(((TypeVariable)signature.resultType()).getType());
			} else {
				node.setType(signature.resultType());
			}
			List<Type> signatureTypes = Arrays.asList(child.getType(), signature.resultType());
			signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
			node.setSignature(signature);
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	
	@Override
	public void visitLeave(ReduceStatementNode node) {
		assert node.nChildren() == 2;
		ParseNode child1  = node.child(0);
		ParseNode child2  = node.child(1);
		List<Type> childTypes = Arrays.asList(child1.getType(), child2.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
		
		if(signature.accepts(childTypes)) {
			if (signature.resultType() instanceof TypeVariable) {
				node.setType(((TypeVariable)signature.resultType()).getType());
			} else {
				node.setType(signature.resultType());
			}
			List<Type> signatureTypes = Arrays.asList(child1.getType(), child2.getType(), signature.resultType());
			signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
			node.setSignature(signature);
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	
	@Override
	public void visitLeave(MapStatementNode node) {
		assert node.nChildren() == 2;
		ParseNode child1  = node.child(0);
		ParseNode child2  = node.child(1);
		List<Type> childTypes = Arrays.asList(child1.getType(), child2.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
		
		if(signature.accepts(childTypes)) {
			if (signature.resultType() instanceof TypeVariable) {
				node.setType(((TypeVariable)signature.resultType()).getType());
			} else {
				node.setType(signature.resultType());
			}
			List<Type> signatureTypes = Arrays.asList(child1.getType(), child2.getType(), signature.resultType());
			signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
			node.setSignature(signature);
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	
	@Override
	public void visitLeave(ZipStatementNode node) {
		assert node.nChildren() == 3;
		ParseNode child1  = node.child(0);
		ParseNode child2  = node.child(1);
		ParseNode child3  = node.child(2);
		List<Type> childTypes = Arrays.asList(child1.getType(), child2.getType(), child3.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
		
		if(signature.accepts(childTypes)) {
			if (signature.resultType() instanceof TypeVariable) {
				node.setType(((TypeVariable)signature.resultType()).getType());
			} else {
				node.setType(signature.resultType());
			}
			List<Type> signatureTypes = Arrays.asList(child1.getType(), child2.getType(), child3.getType(), signature.resultType());
			signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
			node.setSignature(signature);
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	
	@Override
	public void visitLeave(FoldStatementNode node) {
		assert node.nChildren() >= 2;
		ParseNode child1  = node.child(0);
		ParseNode child2  = node.child(1);
		if (node.nChildren() == 2) {
			List<Type> childTypes = Arrays.asList(child1.getType(), child2.getType());
			
			FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
			
			if(signature.accepts(childTypes)) {
				if (signature.resultType() instanceof TypeVariable) {
					node.setType(((TypeVariable)signature.resultType()).getType());
				} else {
					node.setType(signature.resultType());
				}
				List<Type> signatureTypes = Arrays.asList(child1.getType(), child2.getType(), signature.resultType());
				signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
				node.setSignature(signature);
			}
			else {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			}
		} else if (node.nChildren() == 3) {
			ParseNode child3 = node.child(2);
			List<Type> childTypes = Arrays.asList(child1.getType(), child2.getType(), child3.getType());
			
			FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
			
			if(signature.accepts(childTypes)) {
				if (signature.resultType() instanceof TypeVariable) {
					node.setType(((TypeVariable)signature.resultType()).getType());
				} else {
					node.setType(signature.resultType());
				}
				List<Type> signatureTypes = Arrays.asList(child1.getType(), child2.getType(), child3.getType(), signature.resultType());
				signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
				node.setSignature(signature);
			}
			else {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			}
		}
	}
	
	@Override
	public void visitLeave(CloneStatementNode node) {
		assert node.nChildren() == 1;
		ParseNode child  = node.child(0);
		List<Type> childTypes = Arrays.asList(child.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
		
		if(signature.accepts(childTypes)) {
			if (signature.resultType() instanceof TypeVariable) {
				node.setType(((TypeVariable)signature.resultType()).getType());
			} else {
				node.setType(signature.resultType());
			}
			List<Type> signatureTypes = Arrays.asList(child.getType(), signature.resultType());
			signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
			node.setSignature(signature);
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	
	@Override
	public void visitLeave(ReleaseStatementNode node) {
		assert node.nChildren() == 1;
		ParseNode child  = node.child(0);
		List<Type> childTypes = Arrays.asList(child.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
		
		if(signature.accepts(childTypes)) {
			if (signature.resultType() instanceof TypeVariable) {
				node.setType(((TypeVariable)signature.resultType()).getType());
			} else {
				node.setType(signature.resultType());
			}
			List<Type> signatureTypes = Arrays.asList(child.getType(), signature.resultType());
			signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
			node.setSignature(signature);
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	
	@Override
	public void visitLeave(IndexOperatorNode node) {
		assert node.nChildren() == 2;
		ParseNode target  = node.child(0);
		Type targetType = target.getType();
		if (targetType instanceof ArrayType) {
			node.setTargetable(true);
		}
		for (int i = 1; i < node.nChildren(); i++) {
			ParseNode index  = node.child(i);
			List<Type> childTypes = Arrays.asList(targetType, index.getType());
			
			FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
			
			if(signature.accepts(childTypes)) {
				if (signature.resultType() instanceof TypeVariable) {
					node.setType(((TypeVariable)signature.resultType()).getType());
				} else {
					node.setType(signature.resultType());
				}
				node.setSignature(signature);
			}
			else {
				List<Type> promotedTypes = TypePromoter.findPromotions(node.getOperator(), childTypes);
				if (promotedTypes != null) {
					signature = FunctionSignatures.signature(node.getOperator(), promotedTypes);
					if(signature.accepts(promotedTypes)) {
						for (int typeNumber = 0; typeNumber < promotedTypes.size(); typeNumber++) {
							if (promotedTypes.get(typeNumber) != childTypes.get(typeNumber)) {
								ParseNode castNode = TypePromoter.promoteType(node.child(typeNumber), promotedTypes.get(typeNumber));
								node.replaceChild(node.child(typeNumber), castNode);
							}
						}
						if (signature.resultType() instanceof TypeVariable) {
							node.setType(((TypeVariable)signature.resultType()).getType());
						} else {
							node.setType(signature.resultType());
						}
						node.setSignature(signature);
					} else {
						typeCheckError(node, childTypes);
						node.setType(PrimitiveType.ERROR);
					}
				} else {
					typeCheckError(node, childTypes);
					node.setType(PrimitiveType.ERROR);
				}
			}
			if (targetType instanceof ArrayType) {
				targetType = ((ArrayType)targetType).getSubType();
			}
		}
	}
	
	@Override
	public void visitLeave(SubstringStatementNode node) {
		assert node.nChildren() == 3;
		ParseNode stringExpression = node.child(0);
		ParseNode startingIndex = node.child(1);
		ParseNode endingIndex = node.child(2);
		
		List<Type> childTypes = Arrays.asList(stringExpression.getType(), startingIndex.getType(), endingIndex.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
		
		if(signature.accepts(childTypes)) {
			node.setType(signature.resultType());
			node.setSignature(signature);
		} else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// expressions
	@Override
	public void visitLeave(BinaryOperatorNode node) {
		assert node.nChildren() == 2;
		ParseNode left  = node.child(0);
		ParseNode right = node.child(1);
		List<Type> childTypes = Arrays.asList(left.getType(), right.getType());
		
		//Lextant operator = operatorFor(node);
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), childTypes);
		
		if(signature.accepts(childTypes)) {
			if (signature.resultType() instanceof TypeVariable) {
				node.setType(((TypeVariable)signature.resultType()).getType());
			} else {
				node.setType(signature.resultType());
			}
			node.setSignature(signature);
		}
		else {
			List<Type> promotedTypes = TypePromoter.findPromotions(node.getOperator(), childTypes);
			if (promotedTypes != null) {
				signature = FunctionSignatures.signature(node.getOperator(), promotedTypes);
				if(signature.accepts(promotedTypes)) {
					for (int typeNumber = 0; typeNumber < promotedTypes.size(); typeNumber++) {
						if (promotedTypes.get(typeNumber) != childTypes.get(typeNumber)) {
							ParseNode castNode = TypePromoter.promoteType(node.child(typeNumber), promotedTypes.get(typeNumber));
							node.replaceChild(node.child(typeNumber), castNode);
						}
					}
					if (signature.resultType() instanceof TypeVariable) {
						node.setType(((TypeVariable)signature.resultType()).getType());
					} else {
						node.setType(signature.resultType());
					}
					node.setSignature(signature);
				} else {
					typeCheckError(node, childTypes);
					node.setType(PrimitiveType.ERROR);
				}
			} else {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			}
		}
	}
	private Lextant operatorFor(BinaryOperatorNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}
	
	@Override
	public void visitLeave(CastOperatorNode node) {
		assert node.nChildren() == 2;
		ParseNode child  = node.child(0);
		ParseNode typeLiteral = node.child(1);
		Type type = PrimitiveType.NO_TYPE;
		if (typeLiteral instanceof TypeLiteralNode) {
			type = ((TypeLiteralNode)typeLiteral).getCompoundType();
		}
		List<Type> types = Arrays.asList(type, child.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), types);
		
		if(signature.accepts(types)) {
			List<Type> signatureTypes = Arrays.asList(child.getType(), signature.resultType());
			signature = FunctionSignatures.addSignature(node.getOperator(), signatureTypes);
			node.setSignature(signature);
			node.setType(signature.resultType());
		} else {
			typeCheckError(node, types);
			node.setType(PrimitiveType.ERROR);
		}
	}

	
	public void visitEnter(NotOperatorNode node) {
	}
	public void visitLeave(NotOperatorNode node) {
		assert node.nChildren() == 1;
		ParseNode child  = node.child(0);
		List<Type> types = Arrays.asList(child.getType());
		
		FunctionSignature signature = FunctionSignatures.signature(node.getOperator(), types);
		
		if(!signature.accepts(types)) {
			typeCheckError(node, types);
			node.setType(PrimitiveType.ERROR);
		} else {
			if (signature.resultType() instanceof TypeVariable) {
				node.setType(((TypeVariable)signature.resultType()).getType());
			} else {
				node.setType(signature.resultType());
			}
			node.setSignature(signature);
		}
	}

	public void visitLeave(ArrayExpressionNode node) {
		node.setTargetable(true);
		if (node.child(0) instanceof TypeLiteralNode) {
			TypeLiteralNode typeLiteral = (TypeLiteralNode)node.child(0);
			Type t = typeLiteral.getCompoundType();
			if (t instanceof ArrayType && ((ArrayType)t).getRootType() instanceof VoidType) {
				typeCheckError(node, Arrays.asList(t));
				node.setType(PrimitiveType.ERROR);
			} else {
				List<Type> types = Arrays.asList(t);
				ParseNode length = node.child(1);
				FunctionSignature signature = FunctionSignatures.signature(Keyword.NEW, types);
				
				if(!signature.accepts(types)) {
					typeCheckError(node, types);
					node.setType(PrimitiveType.ERROR);
				} else {
					if (signature.resultType() instanceof TypeVariable) {
						node.setType(((TypeVariable)signature.resultType()).getType());
					} else {
						node.setType(signature.resultType());
					}
					node.setSignature(signature);
				}
			}
		} else {
			ParseNode child  = node.child(0);
			int childIndex = 0;
			ArrayType type = new ArrayType(child.getType());
			List<Type> types = Arrays.asList(type);
			type.setSubType(child.getType());
			FunctionSignature signature = FunctionSignatures.signature(Keyword.NEW, types);
			//node.setType(type);
			boolean validForAllChildren = true;
			for (childIndex = 0; childIndex < node.nChildren(); childIndex++) {
				ParseNode tempChild = node.child(childIndex);
				type = new ArrayType(tempChild.getType());
				types = Arrays.asList(type);
				if (!signature.accepts(types)) {
					validForAllChildren = false;
					break;
				}
			}
			
			if(!validForAllChildren) {
				ArrayList<Type> rootTypes = new ArrayList<Type>();
				for (childIndex = 0; childIndex < node.nChildren(); childIndex++) {
					ParseNode tempChild = node.child(childIndex);
					if (tempChild instanceof ArrayExpressionNode) {
						rootTypes.add(((ArrayType)tempChild.getType()).getRootType());
					} else if (tempChild.getType() instanceof PrimitiveType) {
						rootTypes.add(tempChild.getType());
					} else {
						rootTypes.add(PrimitiveType.ERROR);
					}
				}
				List<Type> promotedRootTypes = TypePromoter.findArrayExpressionPromotions(rootTypes);
				boolean validPromotedTypes = true;
				if (promotedRootTypes != null && promotedRootTypes.size() == node.nChildren()) {
					if (node.child(0) instanceof ArrayExpressionNode) {
						((ArrayType)node.child(0).getType()).setRootType(promotedRootTypes.get(0));
					} else if (node.child(0).getType() instanceof PrimitiveType) {
						//node.child(0).promoteType(promotedRootTypes.get(0));
					}
					type = new ArrayType(promotedRootTypes.get(0));
					types = Arrays.asList(type);
					signature = FunctionSignatures.signature(Keyword.NEW, types);
					for (childIndex = 0; childIndex < node.nChildren(); childIndex++) {
						ParseNode tempChild = node.child(childIndex);
						if (tempChild instanceof ArrayExpressionNode) {
							((ArrayType)tempChild.getType()).setRootType(promotedRootTypes.get(childIndex));
						} else if (tempChild.getType() instanceof PrimitiveType) {
							if (!tempChild.getType().match(promotedRootTypes.get(0))) {
								ParseNode castNode = TypePromoter.promoteType(node.child(childIndex), promotedRootTypes.get(0));
								node.replaceChild(node.child(childIndex), castNode);
							}
						}
						type = new ArrayType(promotedRootTypes.get(childIndex));
						types = Arrays.asList(type);
						if (!signature.accepts(types)) {
							validPromotedTypes = false;
							break;
						}
					}
				} else {
					validPromotedTypes = false;
				}
				if (!validPromotedTypes) {
					typeCheckError(node, types);
					node.setType(PrimitiveType.ERROR);
				} else {
					if (signature.resultType() instanceof TypeVariable) {
						node.setType(((TypeVariable)signature.resultType()).getType());
					} else {
						node.setType(signature.resultType());
					}
					node.setSignature(signature);
				}
			} else {
				if (signature.resultType() instanceof TypeVariable) {
					node.setType(((TypeVariable)signature.resultType()).getType());
				} else {
					node.setType(signature.resultType());
				}
				node.setSignature(signature);
			}
		}
	}
	
	public void visitLeave(FunctionDefinitionNode node) {
		assert node.nChildren() == 2;
		assert node.child(0) instanceof IdentifierNode;
		assert node.child(1) instanceof LambdaNode;
		
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		LambdaNode lambda = (LambdaNode) node.child(1);
		
		//addBinding(identifier, lambda.getType(), false, true);
	}
	
	public void visitLeave(FunctionInvocationNode node) {
		assert node.nChildren() == 2;
		assert node.child(1) instanceof ExpressionListNode;
		
		if (node.child(0) instanceof IdentifierNode) {
			IdentifierNode identifier = (IdentifierNode) node.child(0);
			ExpressionListNode parameters = (ExpressionListNode) node.child(1);
			Binding declaration = getDeclaration(identifier);
			List<Type> parameterTypes = parameters.getParameterTypes();
			if (declaration == null) {
				noPriorDeclarationError(identifier);
				node.setType(PrimitiveType.ERROR);
			}
			if (declaration != null && declaration.getType() instanceof LambdaType) {
				LambdaType type = (LambdaType) declaration.getType();
				FunctionSignature signature = type.getSignature();
				if (signature.accepts(parameterTypes)) {
					node.setType(signature.resultType());
				} else {
					boolean foundValidPromotion = false;
					for (int typeNumber = 0; typeNumber < parameterTypes.size(); typeNumber++) {
						Type currentType = parameterTypes.get(typeNumber);
						List<Type> promotionTypes = TypePromoter.getImplicitPromotions(currentType);
						for (Type promotion : promotionTypes) {
							parameterTypes.set(typeNumber, promotion);
							if (signature.accepts(parameterTypes)) {
								ParseNode castNode = TypePromoter.promoteType(parameters.child(typeNumber), parameterTypes.get(typeNumber));
								parameters.replaceChild(parameters.child(typeNumber), castNode);
								node.setType(signature.resultType());
								foundValidPromotion = true;
								break;
							}
						}
						if (foundValidPromotion) {
							break;
						} else {
							parameterTypes.set(typeNumber, currentType);
						}
					}
					if (!foundValidPromotion) {
						typeCheckError(node, parameterTypes);
						node.setType(PrimitiveType.ERROR);
					}
				}
			} else {
				typeCheckError(node, parameterTypes);
				node.setType(PrimitiveType.ERROR);
			}
		} else if (node.child(0) instanceof IndexOperatorNode) {
			IndexOperatorNode indexOperator = (IndexOperatorNode) node.child(0);
			IdentifierNode identifier = (IdentifierNode) indexOperator.child(0);
			ExpressionListNode parameters = (ExpressionListNode) node.child(1);
			Binding declaration = getDeclaration(identifier);
			List<Type> parameterTypes = parameters.getParameterTypes();
			if (declaration == null) {
				noPriorDeclarationError(identifier);
				node.setType(PrimitiveType.ERROR);
			}
			if (declaration != null && indexOperator.getType() instanceof LambdaType) {
				LambdaType type = (LambdaType) indexOperator.getType();
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
		} else {
			ParseNode expression = node.child(0);
			ExpressionListNode parameters = (ExpressionListNode) node.child(1);
			List<Type> parameterTypes = parameters.getParameterTypes();
			if (expression.getType() instanceof LambdaType) {
				LambdaType type = (LambdaType) expression.getType();
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
		}
		
	}
	
	public void visitLeave(CallStatementNode node) {
		assert node.nChildren() == 1;
		ParseNode function = node.child(0);
		if (!(function instanceof FunctionInvocationNode)) {
			node.setType(PrimitiveType.ERROR);
			typeCheckError(node, Arrays.asList(function.getType()));
		}
	}
	
	public void visitEnter(LambdaNode node) {
		//enterLambdaScope(node);
	}
	
	public void visitLeave(LambdaNode node) {
		leaveScope(node);
		assert node.nChildren() == 2;
		assert node.child(0) instanceof LambdaParamTypeNode;
		assert node.child(1) instanceof BlockStatementNode;
		
		LambdaParamTypeNode paramTypes = (LambdaParamTypeNode)node.child(0);
		BlockStatementNode block = (BlockStatementNode)node.child(1);
		FunctionSignature signature = null;
		if (paramTypes.getType() instanceof LambdaType) {
			LambdaType lType = (LambdaType) paramTypes.getType();
			signature = lType.getSignature();
			node.setType(lType);
		}
		for (ParseNode child : block.getChildren()) {
			if (child instanceof BlockStatementNode) {
				if (!((BlockStatementNode) child).getReturnType().match(signature.resultType())) {
					node.setType(PrimitiveType.ERROR);
					conflictingReturnTypeError(child, signature.resultType(), ((BlockStatementNode) child).getReturnType());
					break;
				}
			} else if (child instanceof ReturnStatementNode) {
				if (!child.getType().match(signature.resultType())) {
					node.setType(PrimitiveType.ERROR);
					conflictingReturnTypeError(child, signature.resultType(), child.getType());
					break;
				}
			}
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
		
		IdentifierNode returnPointer = new IdentifierNode(IdentifierToken.make(null, "%lambdaReturn"));
		returnPointer.setType(PrimitiveType.INTEGER);
		node.appendChild(returnPointer);
		IdentifierNode dynamicLink = new IdentifierNode(IdentifierToken.make(null, "%lambdaDynLink"));
		dynamicLink.setType(PrimitiveType.INTEGER);
		node.appendChild(dynamicLink);
		
		addBinding(dynamicLink, PrimitiveType.INTEGER, false);
		addBinding(returnPointer, PrimitiveType.INTEGER, false);
	}
	
	public void visitLeave(ParameterSpecificationNode node) {
		assert node.nChildren() == 2;
		assert node.child(0) instanceof TypeLiteralNode;
		assert node.child(1) instanceof IdentifierNode;
		
		TypeLiteralNode typeLiteral = (TypeLiteralNode)node.child(0);
		Type t = typeLiteral.getCompoundType();
		//IdentifierNode identifier = (IdentifierNode)node.child(1);
		//identifier.setType(t);
		//node.setType(t);
		//addBinding(identifier, t, true);
		if (t instanceof VoidType || (t instanceof ArrayType && ((ArrayType)t).getRootType() instanceof VoidType)) {
			typeCheckError(node, Arrays.asList(t));
			node.setType(PrimitiveType.ERROR);
		} else {
			IdentifierNode identifier = (IdentifierNode)node.child(1);
			identifier.setType(t);
			node.setType(t);
			//addBinding(identifier, t, true);
		}
	}
	
	public void visitLeave(ReturnStatementNode node) {
		if (node.nChildren() == 1) {
			Type returnType = node.child(0).getType();
			node.setType(returnType);
		} else if (node.nChildren() == 0) {
			node.setType(new VoidType());
		}
		
		ParseNode temp = node.getParent();
		while (temp.getParent() != null) {
			if (temp instanceof LambdaNode) { 
				break;
			}
			temp = temp.getParent();
		}
		if (temp != null && temp instanceof LambdaNode) {
			//handling return promotions
			if (node.nChildren() >= 1 && !temp.getSignature().resultType().match(node.child(0).getType())) {
				List<Type> promotedTypes = TypePromoter.getImplicitPromotions(node.child(0).getType());
				if (promotedTypes != null) {
					for (int typeNumber = 0; typeNumber < promotedTypes.size(); typeNumber++) {
						if (temp.getSignature().resultType().match(promotedTypes.get(typeNumber))) {
							//ParseNode castNode = TypePromoter.promoteType(node.child(0), promotedTypes.get(typeNumber));
							//node.replaceChild(node.child(0), castNode);
							//node.setType(castNode.getType());
							break;
						} else {
							node.setType(PrimitiveType.ERROR);
							typeCheckError(node, Arrays.asList(node.child(0).getType()));
						}
					}
				} else {
					node.setType(PrimitiveType.ERROR);
					typeCheckError(node, Arrays.asList(node.child(0).getType()));
				}
			}
			
			//adding the dynamic link and return location to the return node
			for (ParseNode child : temp.getChildren()) {
				if (child instanceof LambdaParamTypeNode) {
					node.appendChild(child.child(2));
					node.appendChild(child.child(3));
				}
			}
		} else {
			node.setType(PrimitiveType.ERROR);
			couldNotFindFunctionParentError(node);
		}
	}
	
	public void visitLeave(BreakStatementNode node) {
		ParseNode parent = node.getParent();
		while (parent != null) {
			if (parent instanceof WhileStatementNode || parent instanceof ForStatementNode) {
				break;
			} else {
				parent = parent.getParent();
			}
		}
		if (parent != null) {
			node.setControlFlowParent(parent);
		} else {
			node.setType(PrimitiveType.ERROR);
			couldNotFindControlFlowParentError(node);
		}
	}
	
	public void visitLeave(ContinueStatementNode node) {
		ParseNode parent = node.getParent();
		while (parent != null) {
			if (parent instanceof WhileStatementNode || parent instanceof ForStatementNode) {
				break;
			} else {
				parent = parent.getParent();
			}
		}
		if (parent != null) {
			node.setControlFlowParent(parent);
		} else {
			node.setType(PrimitiveType.ERROR);
			couldNotFindControlFlowParentError(node);
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
	@Override
	public void visit(IdentifierNode node) {
		node.setTargetable(true);
		if(!isBeingDeclared(node)) {		
			Binding binding = node.findVariableBinding();
			
			node.setType(binding.getType());
			node.setBinding(binding);
			node.setMutable(binding.getMutable());
			node.setStatic(binding.getStatic());
		}
		// else parent DeclarationNode does the processing.
	}
	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return ((parent instanceof DeclarationNode) && (node == parent.child(0))) 
				|| (parent instanceof FunctionDefinitionNode)
				|| (parent instanceof ParameterSpecificationNode)
				|| (parent instanceof ForStatementIncrementNode);
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
	private void IndexableTypeCheckError(ParseNode expression) {
		Token token = expression.getToken();
		
		logError("expression " + token.getLexeme() + " is not an indexable type; " + " at " + token.getLocation());	
	}
	private void ForLoopError(ParseNode loop) {
		Token token = loop.getToken();
		
		logError("for loop is not an index loop nor an elem loop; " + " at " + token.getLocation());	
	}
	private void noPriorDeclarationError(ParseNode node) {
		Token token = node.getToken();
		
		logError("identifier " + token.getLexeme() + " was not previously declared at " + token.getLocation());	
	}
	private void expressionIsNotTargetableError(ParseNode node) {
		Token token = node.getToken();
		
		logError("expression " + token.getLexeme() + " is not targetable; Error at " + token.getLocation());
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
	private void couldNotFindFunctionParentError(ParseNode currentNode) {
		Token token = currentNode.getToken();
		logError("could not find the function parent for the " + token.getLexeme() + " statement at " + token.getLocation());
	}
	private void logError(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
}