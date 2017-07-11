package asmCodeGenerator;

import java.util.HashMap;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.MemoryManager;
import asmCodeGenerator.runtime.RunTime;
import asmCodeGenerator.runtime.SubRoutines;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
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
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.VoidType;
import symbolTable.Binding;
import symbolTable.MemoryAllocator;
import symbolTable.NegativeMemoryAllocator;
import symbolTable.Scope;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
	ParseNode root;

	public static ASMCodeFragment generate(ParseNode syntaxTree) {
		ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
		return codeGenerator.makeASM();
	}
	public ASMCodeGenerator(ParseNode root) {
		super();
		this.root = root;
	}
	
	public ASMCodeFragment makeASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.append( MemoryManager.codeForInitialization() );
		code.append( framePointerASM() );
		code.append( stackPointerASM() );
		code.append( RunTime.getEnvironment() );
		code.append( globalVariableBlockASM() );
		code.append( programASM() );
		code.append( SubRoutines.getEnvironment() );
		code.append( MemoryManager.codeForAfterApplication() );
		
		return code;
	}
	private ASMCodeFragment globalVariableBlockASM() {
		assert root.hasScope();
		Scope scope = root.getScope();
		int globalBlockSize = scope.getAllocatedSize();
		
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.GLOBAL_MEMORY_BLOCK);
		code.add(DataZ, globalBlockSize);
		return code;
	}
	private ASMCodeFragment framePointerASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.FRAME_POINTER);
		code.add(DataI, 0);
		code.add(PushD, RunTime.FRAME_POINTER);
		code.add(Memtop);
		code.add(StoreI);
		return code;
	}
	private ASMCodeFragment stackPointerASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.STACK_POINTER);
		code.add(DataI, 0);
		code.add(PushD, RunTime.STACK_POINTER);
		code.add(Memtop);
		code.add(StoreI);
		return code;
	}
	private ASMCodeFragment programASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		
		code.add(    Label, RunTime.MAIN_PROGRAM_LABEL);
		code.append( programCode());
		code.add(    Halt );
		
		return code;
	}
	private ASMCodeFragment programCode() {
		CodeVisitor visitor = new CodeVisitor();
		root.accept(visitor);
		return visitor.removeRootCode(root);
	}


	protected class CodeVisitor extends ParseNodeVisitor.Default {
		private Map<ParseNode, ASMCodeFragment> codeMap;
		ASMCodeFragment code;
		
		public CodeVisitor() {
			codeMap = new HashMap<ParseNode, ASMCodeFragment>();
		}


		////////////////////////////////////////////////////////////////////
        // Make the field "code" refer to a new fragment of different sorts.
		private void newAddressCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_ADDRESS);
			codeMap.put(node, code);
		}
		private void newValueCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VALUE);
			codeMap.put(node, code);
		}
		private void newVoidCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VOID);
			codeMap.put(node, code);
		}

	    ////////////////////////////////////////////////////////////////////
        // Get code from the map.
		private ASMCodeFragment getAndRemoveCode(ParseNode node) {
			ASMCodeFragment result = codeMap.get(node);
			codeMap.remove(result);
			return result;
		}
	    public  ASMCodeFragment removeRootCode(ParseNode tree) {
			return getAndRemoveCode(tree);
		}		
		ASMCodeFragment removeValueCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			makeFragmentValueCode(frag, node);
			return frag;
		}		
		private ASMCodeFragment removeAddressCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isAddress();
			return frag;
		}		
		ASMCodeFragment removeVoidCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isVoid();
			return frag;
		}
		
	    ////////////////////////////////////////////////////////////////////
        // convert code to value-generating code.
		private void makeFragmentValueCode(ASMCodeFragment code, ParseNode node) {
			assert !code.isVoid();
			if(code.isAddress()) {
				turnAddressIntoValue(code, node);
			}	
		}
		private void turnAddressIntoValue(ASMCodeFragment code, ParseNode node) {
			if (node instanceof IdentifierNode && ((IdentifierNode)node).isStatic()) {
				code.add(PushI, 1);
				code.add(Add);
			}
				
			if(node.getType() == PrimitiveType.INTEGER) {
				code.add(LoadI);
			}	
			else if(node.getType() == PrimitiveType.BOOLEAN) {
				code.add(LoadC);
			}
			else if(node.getType() == PrimitiveType.FLOAT) {
				code.add(LoadF);
			}
			else if(node.getType() == PrimitiveType.STRING) {
				code.add(LoadI);
			}
			else if(node.getType() == PrimitiveType.CHARACTER) {
				code.add(LoadC);
			}
			else if(node.getType() instanceof ArrayType) {
				code.add(LoadI);
			}
			else if(node.getType() == PrimitiveType.RATIONAL) {
				code.add(Duplicate);
				code.add(PushI, 4);
				code.add(Add);
				code.add(LoadI);
				code.add(Exchange);
				code.add(LoadI);
			}
			else if(node.getType() instanceof LambdaType) {
				code.add(LoadI);
			}
			else {
				assert false : "node " + node;
			}
			code.markAsValue();
		}
		
	    ////////////////////////////////////////////////////////////////////
        // ensures all types of ParseNode in given AST have at least a visitLeave	
		public void visitLeave(ParseNode node) {
			assert false : "node " + node + " not handled in ASMCodeGenerator";
		}
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		public void visitLeave(ProgramNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}
		public void visitLeave(MainBlockNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}
		public void visitLeave(BlockStatementNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}

		///////////////////////////////////////////////////////////////////////////
		// statements and declarations

		public void visitLeave(PrintStatementNode node) {
			newVoidCode(node);
			new PrintStatementGenerator(code, this).generate(node);	
		}
		public void visit(NewlineNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.NEWLINE_PRINT_FORMAT);
			code.add(Printf);
		}
		public void visit(TabNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.TAB_PRINT_FORMAT);
			code.add(Printf);
		}
		public void visit(SpaceNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.SPACE_PRINT_FORMAT);
			code.add(Printf);
		}
		

		public void visitLeave(DeclarationNode node) {
			newVoidCode(node);
			Labeller staticLabeller = new Labeller("static-init");
			String startStaticInit = staticLabeller.newLabel("start");
			String endStaticInit = staticLabeller.newLabel("end");
			IdentifierNode identifier = null;
			if (node.child(0) instanceof IdentifierNode) {
				identifier = (IdentifierNode) node.child(0);
			}
			if (identifier != null && identifier.isStatic()) {
				code.add(Label, startStaticInit);
			}
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));	
			ASMCodeFragment rvalue;
			//if (node.child(1).getType() instanceof LambdaType) {
			//	rvalue = removeVoidCode(node.child(1));
			//} else {
			rvalue = removeValueCode(node.child(1));
			//}
			Type type = node.getType();
			ASMOpcode storeCode = opcodeForStore(type);
			
			code.append(lvalue.makeCopy());
			if (identifier != null && identifier.isStatic()) {
				code.add(Duplicate);
				code.add(LoadC);
				code.add(PushI, 1);
				code.add(Subtract);
				code.add(JumpFalse, endStaticInit);
				code.add(Duplicate);
				code.add(PushI, 1);
				code.add(StoreC);
				code.add(PushI, 1);
				code.add(Add);
			}
			
			if (node.getType() == PrimitiveType.RATIONAL) {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(Exchange);
				code.add(StoreI);
			}
			
			code.append(rvalue.makeCopy());
			
			if (node.getType() == PrimitiveType.RATIONAL) {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(Exchange);
				code.add(storeCode);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(PushI, 4);
				code.add(Add);
				code.add(Exchange);
			}
			//if (node.getType() instanceof LambdaType) {
			//	code.add(PushD, ((LambdaNode)node.child(1)).getStartLabel());
			//}
			code.add(storeCode);
			if (identifier != null && identifier.isStatic()) {
				code.add(Label, endStaticInit);
			}
		}
		public void visitLeave(AssignmentNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));	
			ASMCodeFragment rvalue;
			IdentifierNode identifier = null;
			if (node.child(0) instanceof IdentifierNode) {
				identifier = (IdentifierNode) node.child(0);
			}
			rvalue = removeValueCode(node.child(1));
			Type type = node.getType();
			ASMOpcode storeCode = opcodeForStore(type);
			
			code.append(lvalue.makeCopy());
			if (identifier != null && identifier.isStatic()) {
				code.add(PushI, 1);
				code.add(Add);
			}
			if (node.getType() == PrimitiveType.RATIONAL) {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(Exchange);
				code.add(StoreI);
			}
			
			code.append(rvalue.makeCopy());
			
			if (node.getType() == PrimitiveType.RATIONAL) {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(Exchange);
				code.add(storeCode);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(PushI, 4);
				code.add(Add);
				code.add(Exchange);
			}
			
			code.add(storeCode);
		}
		private ASMOpcode opcodeForStore(Type type) {
			if(type == PrimitiveType.INTEGER) {
				return StoreI;
			}
			if(type == PrimitiveType.STRING) {
				return StoreI;
			}
			if(type instanceof ArrayType) {
				return StoreI;
			}
			if(type == PrimitiveType.FLOAT) {
				return StoreF;
			}
			if(type == PrimitiveType.BOOLEAN) {
				return StoreC;
			}
			if(type == PrimitiveType.CHARACTER) {
				return StoreC;
			}
			if(type == PrimitiveType.RATIONAL) {
				return StoreI;
			}
			if(type instanceof LambdaType) {
				return StoreI;
			}
			assert false: "Type " + type + " unimplemented in opcodeForStore()";
			return null;
		}
		
		private ASMCodeFragment fragmentForStore(Type type) {
			ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
			if(type == PrimitiveType.INTEGER) {
				frag.add(StoreI);
			} else if(type == PrimitiveType.STRING) {
				frag.add(StoreI);
			} else if(type instanceof ArrayType) {
				frag.add(StoreI);
			} else if(type == PrimitiveType.FLOAT) {
				frag.add(StoreF);
			} else if(type == PrimitiveType.BOOLEAN) {
				frag.add(StoreC);
			} else if(type == PrimitiveType.CHARACTER) {
				frag.add(StoreC);
			} else if(type == PrimitiveType.RATIONAL) {
				frag.add(StoreI);
				frag.add(PushI, 4);
				frag.add(Add);
				frag.add(StoreI);
			} else {
				assert false: "Type " + type + " unimplemented in opcodeForStore()";
				return null;
			}
			return frag;
		}
		
		private ASMOpcode opcodeForLoad(Type type) {
			if(type == PrimitiveType.INTEGER) {
				return LoadI;
			}	
			else if(type == PrimitiveType.BOOLEAN) {
				return LoadC;
			}
			else if(type == PrimitiveType.FLOAT) {
				return LoadF;
			}
			else if(type == PrimitiveType.STRING) {
				return LoadI;
			}
			else if(type == PrimitiveType.CHARACTER) {
				return LoadC;
			}
			else if(type instanceof ArrayType) {
				return LoadI;
			} 
			else if (type == PrimitiveType.RATIONAL) { 
				return LoadI;
			}
			else if (type instanceof LambdaType) {
				return LoadI;
			}
			assert false: "Type " + type + " unimplemented in opcodeForLoad()";
			return null;
		}
		
		private ASMCodeFragment fragmentForLoad(Type type) {
			ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
			if(type == PrimitiveType.INTEGER) {
				frag.add(LoadI);
			} else if(type == PrimitiveType.STRING) {
				frag.add(LoadI);
			} else if(type instanceof ArrayType) {
				frag.add(LoadI);
			} else if(type == PrimitiveType.FLOAT) {
				frag.add(LoadF);
			} else if(type == PrimitiveType.BOOLEAN) {
				frag.add(LoadC);
			} else if(type == PrimitiveType.CHARACTER) {
				frag.add(LoadC);
			} else if(type == PrimitiveType.RATIONAL) {
				frag.add(Duplicate);
				frag.add(PushI, 4);
				frag.add(Add);
				frag.add(LoadI);
				frag.add(Exchange);
				frag.add(LoadI);
				
			} else {
				assert false: "Type " + type + " unimplemented in opcodeForStore()";
				return null;
			}
			return frag;
		}

		public void visitLeave(IfStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment conditional = removeValueCode(node.child(0));
			ASMCodeFragment blockStatement = removeVoidCode(node.child(1));
			ASMCodeFragment elseStatement = null;
			if (node.nChildren() > 2) {
				elseStatement = removeVoidCode(node.child(2));
			}
			
			Labeller labeller = new Labeller("ifstatement");
			String startLabel = labeller.newLabel("start");
			String elseLabel = labeller.newLabel("else");
			String endLabel = labeller.newLabel("end");
			
			code.add(Label, startLabel);
			code.append(conditional.makeCopy());
			if (elseStatement == null) {
				code.add(JumpFalse, endLabel);
			} else {
				code.add(JumpFalse, elseLabel);
			}
			code.append(blockStatement.makeCopy());
			code.add(Jump, endLabel);
			if (elseStatement != null) {
				code.add(Label, elseLabel);
				code.append(elseStatement.makeCopy());
				code.add(Jump, endLabel);
			}
			code.add(Label, endLabel);
		}

		public void visitLeave(ElseStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment blockStatement = removeVoidCode(node.child(0));
			code.append(blockStatement.makeCopy());
		}
		
		public void visitEnter(WhileStatementNode node) {
			Labeller labeller = new Labeller("whilestatement");
			String startLabel = labeller.newLabel("start");
			String endLabel = labeller.newLabel("end");
			node.setStartLabel(startLabel);
			node.setEndLabel(endLabel);
		}
		
		public void visitLeave(WhileStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment conditional = removeValueCode(node.child(0));
			ASMCodeFragment blockStatement = removeVoidCode(node.child(1));

			String startLabel = node.getStartLabel();
			String endLabel = node.getEndLabel();
			
			code.add(Label, startLabel);
			code.append(conditional.makeCopy());
			code.add(JumpFalse, endLabel);
			code.append(blockStatement.makeCopy());
			code.add(Jump, startLabel);
			code.add(Label, endLabel);
		}
		
		public void visitLeave(ForStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment header = removeVoidCode(node.child(0));
			ASMCodeFragment blockStatement = removeVoidCode(node.child(1));

			String startLabel = node.getStartLabel();
			String startIncrementLabel = node.getStartIncrementLabel();
			String endLabel = node.getEndLabel();
			
			code.add(Label, startLabel);
			code.append(header.makeCopy());
			code.append(blockStatement.makeCopy());
			code.add(Jump, startIncrementLabel);
			code.add(Label, endLabel);
		}
		
		public void visitLeave(ForStatementIncrementNode node) {
			newVoidCode(node);
			ASMCodeFragment identifier = removeAddressCode(node.child(0));
			ASMCodeFragment expression_base = removeAddressCode(node.child(1));
			ASMCodeFragment index_base = removeAddressCode(node.child(2));
			ASMCodeFragment index;
			ASMCodeFragment expression;
			
			Type targetType = node.child(0).getType();
			
			index = index_base.makeCopy();
			code.append(index); //Index
			code.add(PushI, -1);
			code.add(StoreI);
			
			code.add(Label, node.getStartIncrementLabel());
			index = index_base.makeCopy();
			code.append(index); //Index
			index = index_base.makeCopy();
			code.append(index); //Index
			code.add(LoadI);
			code.add(PushI, 1);
			code.add(Add);
			code.add(StoreI);
			
			expression = expression_base.makeCopy(); //Target
			code.append(expression);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			index = index_base.makeCopy();
			code.append(index); //Index
			code.add(LoadI);
			code.add(Subtract);
			code.add(JumpFalse, node.getEndLabel());
			
			code.append(identifier);
			if (node.getElemLoop()) {
				expression = expression_base.makeCopy(); //Target
				code.append(expression);
				code.add(LoadI);
				if (node.child(1).getType() instanceof ArrayType) {
					code.add(PushI, ArrayType.headerSize());
					code.add(Add);
				} else if (node.child(1).getType() == PrimitiveType.STRING) {
					code.add(PushI, PrimitiveType.STRING.getHeaderSize());
					code.add(Add);
				}
				index = index_base.makeCopy();
				code.append(index); //Index
				code.add(LoadI);
				code.add(PushI, targetType.getSize());
				code.add(Multiply);
				code.add(Add);
				ASMOpcode loadCode = opcodeForLoad(targetType);
				code.add(loadCode);
			} else if (node.getIndexLoop()) {
				index = index_base.makeCopy();
				code.append(index); //Index
				code.add(LoadI);
			}
			ASMOpcode storeCode = opcodeForStore(targetType);
			code.add(storeCode);
			code.add(Label, node.getEndIncrementLabel());
		}
		///////////////////////////////////////////////////////////////////////////
		// expressions
		public void visitLeave(CastOperatorNode node) {
			newValueCode(node);
			ASMCodeFragment expression = removeValueCode(node.child(0));
			code.append(expression.makeCopy());
			generateCastOpcode(node.getType(), node.child(0).getType());
		}
		
		public void visitLeave(LengthStatementNode node) {
			newValueCode(node);
			ASMCodeFragment expression = removeValueCode(node.child(0));
			code.append(expression.makeCopy());
			if (node.child(0).getType() instanceof ArrayType) {
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
			} else if (node.child(0).getType() instanceof PrimitiveType &&
					node.child(0).getType() == PrimitiveType.STRING) {
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
			}
			code.add(LoadI);
		}
		
		public void visitLeave(ReverseStatementNode node) {
			newValueCode(node);
			ASMCodeFragment arrayExpression = removeValueCode(node.child(0));
			ASMCodeFragment stringExpression = arrayExpression;
			
			Labeller reverseLabeller = new Labeller("reverse-statement");
			String startReverse = reverseLabeller.newLabel("start");
			String endReverse = reverseLabeller.newLabel("end");
			Type storeType;
			Type loadType;
			if (node.getType() instanceof ArrayType) {
				storeType = ((ArrayType)node.getType()).getSubType();
				loadType = ((ArrayType)node.child(0).getType()).getSubType();
				
				ASMOpcode storeCode = opcodeForStore(storeType);
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.append(arrayExpression.makeCopy());
				code.add(StoreI);
				
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(PushI, ArrayType.headerSize());
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushI, storeType.getSize());
				code.add(Multiply);
				code.add(Add);
				code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
				code.add(StoreI);
				
				int memLocation = 0;
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(Duplicate);
				code.add(PushI, memLocation);
				code.add(Add);
				code.add(PushI, 7);
				code.add(StoreI);
				memLocation += 4;
				code.add(Duplicate);
				code.add(PushI, memLocation);
				code.add(Add);
				if (node.getType() instanceof ArrayType && ((ArrayType)node.getType()).getSubType() instanceof ArrayType) {
					code.add(PushI, 2);
				} else {
					code.add(PushI, 0);
				}
				code.add(StoreI);
				memLocation += 4;
				code.add(Duplicate);
				code.add(PushI, memLocation);
				code.add(Add);
				code.add(PushI, ((ArrayType)node.getType()).getSubtypeSize());
				code.add(StoreI);
				memLocation += 4;
				code.add(Duplicate);
				code.add(PushI, memLocation);
				code.add(Add);
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(StoreI);
				memLocation += 4;
				
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(PushI, 0);
				code.add(StoreI);
				
				code.add(PushD, RunTime.GLOBAL_COUNTER_2);
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Subtract);
				code.add(StoreI);
				
				//start
				code.add(Label, startReverse);
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(LoadI);
				code.add(Subtract);
				code.add(JumpFalse, endReverse);
				code.add(PushD, RunTime.GLOBAL_COUNTER_2);
				code.add(LoadI);
				code.add(JumpNeg, endReverse);
				
				//body
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(PushI, ArrayType.headerSize());
				code.add(Add);
				code.add(PushD, RunTime.GLOBAL_COUNTER_2);
				code.add(LoadI);
				code.add(PushI, storeType.getSize());
				code.add(Multiply);
				code.add(Add);
				code.add(StoreI);
				
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.headerSize());
				code.add(Add);
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(LoadI);
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.subtypeSizeOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(Multiply);
				code.add(Add);
				if (loadType == PrimitiveType.RATIONAL) {
					code.add(Duplicate);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
					code.add(PushI, 4);
					code.add(Add);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
				} else {
					code.add(opcodeForLoad(loadType));
				}
				if (storeType == PrimitiveType.RATIONAL) {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
					code.add(LoadI);
					code.add(Exchange);
					code.add(storeCode);
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
					code.add(LoadI);
					code.add(PushI, 4);
					code.add(Add);
					code.add(Exchange);
				} else {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
					code.add(LoadI);
					code.add(Exchange);
				}
				code.add(storeCode);
				
				//increment
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(Duplicate);
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Add);
				code.add(StoreI);
				code.add(PushD, RunTime.GLOBAL_COUNTER_2);
				code.add(Duplicate);
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Subtract);
				code.add(StoreI);
				code.add(Jump, startReverse);
				code.add(Label, endReverse);
			} else {
				storeType = PrimitiveType.CHARACTER;
				loadType = PrimitiveType.CHARACTER;
				
				ASMOpcode storeCode = opcodeForStore(storeType);
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.append(stringExpression.makeCopy());
				code.add(StoreI);
				
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(PushI, PrimitiveType.STRING.getHeaderSize());
				//code.append(stringExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(Add);
				code.add(PushI, 1);
				code.add(Add);
				code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
				code.add(StoreI);
				
				int memLocation = 0;
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(Duplicate);
				code.add(PushI, memLocation);
				code.add(Add);
				code.add(PushI, 6);
				code.add(StoreI);
				memLocation += 4;
				code.add(Duplicate);
				code.add(PushI, memLocation);
				code.add(Add);
				code.add(PushI, 9);
				code.add(StoreI);
				memLocation += 4;
				code.add(Duplicate);
				code.add(PushI, memLocation);
				code.add(Add);
				//code.append(stringExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(StoreI);
				memLocation += 4;
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(PushI, PrimitiveType.STRING.getHeaderSize());
				//code.append(stringExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(Add);
				code.add(Add);
				code.add(PushI, 1);
				code.add(Add);
				code.add(PushI, 0);
				code.add(storeCode);
				
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(PushI, 0);
				code.add(StoreI);
				
				code.add(PushD, RunTime.GLOBAL_COUNTER_2);
				//code.append(stringExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Subtract);
				code.add(StoreI);
				
				//start
				code.add(Label, startReverse);
				//code.append(stringExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(LoadI);
				code.add(Subtract);
				code.add(JumpFalse, endReverse);
				code.add(PushD, RunTime.GLOBAL_COUNTER_2);
				code.add(LoadI);
				code.add(JumpNeg, endReverse);
				
				//body
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(PushI, PrimitiveType.STRING.getHeaderSize());
				code.add(Add);
				code.add(PushD, RunTime.GLOBAL_COUNTER_2);
				code.add(LoadI);
				code.add(PushI, storeType.getSize());
				code.add(Multiply);
				code.add(Add);
				code.add(StoreI);
				
				//code.append(stringExpression.makeCopy());
				code.add(PushD, RunTime.REVERSE_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, PrimitiveType.STRING.getHeaderSize());
				code.add(Add);
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(LoadI);
				code.add(Add);
				code.add(opcodeForLoad(loadType));

				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(Exchange);
				code.add(storeCode);
				
				//increment
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(Duplicate);
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Add);
				code.add(StoreI);
				code.add(PushD, RunTime.GLOBAL_COUNTER_2);
				code.add(Duplicate);
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Subtract);
				code.add(StoreI);
				code.add(Jump, startReverse);
				code.add(Label, endReverse);
			}
		}
		
		public void visitLeave(ReduceStatementNode node) {
			newValueCode(node);
			ASMCodeFragment arrayExpression = removeValueCode(node.child(0));
			ASMCodeFragment lambdaExpression = removeValueCode(node.child(1));
			
			Labeller reduceLabeller = new Labeller("reduce-statement");
			String startFindValidArrayElements = reduceLabeller.newLabel("start-find");
			String endFindValidArrayElements = reduceLabeller.newLabel("end-find");
			String setIsValidElement = reduceLabeller.newLabel("set-valid-true");
			String setIsNotValidElement = reduceLabeller.newLabel("set-valid-false");
			String startReducing = reduceLabeller.newLabel("start");
			String endReducing = reduceLabeller.newLabel("end");
			String storeValidElement = reduceLabeller.newLabel("store-valid-true");
			String storeNotValidElement = reduceLabeller.newLabel("store-valid-false");
			
			Type storeType = ((ArrayType)node.getType()).getSubType();
			Type loadType = ((ArrayType)node.child(0).getType()).getSubType();
			ASMOpcode storeCode = opcodeForStore(storeType);
			
			code.add(PushD, RunTime.REDUCE_ARRAY_EXPRESSION_POINTER);
			code.append(arrayExpression.makeCopy());
			code.add(StoreI);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_3); //valid indexes to include in the reduced array (0 is invalid, 1 is valid)
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.REDUCE_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			code.add(StoreI);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_INTEGER_1);
			code.add(PushI, 0);
			code.add(StoreI);
			
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(PushI, 0);
			code.add(StoreI);
			
			//start
			code.add(Label, startFindValidArrayElements);
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.REDUCE_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(Subtract);
			code.add(JumpFalse, endFindValidArrayElements);
			
			//body
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(PushI, storeType.getSize());
			code.add(Multiply);
			code.add(Add);
			code.add(StoreI);
			
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.REDUCE_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.REDUCE_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);
			if (loadType == PrimitiveType.RATIONAL) {
				code.add(Duplicate);
				code.add(opcodeForLoad(loadType));
				code.add(Exchange);
				code.add(PushI, 4);
				code.add(Add);
				code.add(opcodeForLoad(loadType));
				code.add(Exchange);
			} else {
				code.add(opcodeForLoad(loadType));
			}
			code.append(lambdaExpression.makeCopy());
			code.add(CallV);
			code.add(PushI, 1);
			code.add(Subtract);
			code.add(JumpFalse, setIsValidElement);
			
			code.add(Jump, setIsNotValidElement);
			code.add(Label, setIsValidElement);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_3);
			code.add(LoadI);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(Add);
			code.add(PushI, 1);
			code.add(StoreC);
			code.add(PushD, RunTime.GLOBAL_TEMP_INTEGER_1);
			code.add(PushD, RunTime.GLOBAL_TEMP_INTEGER_1);
			code.add(LoadI);
			code.add(PushI, 1);
			code.add(Add);
			code.add(StoreI);
			code.add(Label, setIsNotValidElement);
			
			//increment
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(Duplicate);
			code.add(LoadI);
			code.add(PushI, 1);
			code.add(Add);
			code.add(StoreI);
			code.add(Jump, startFindValidArrayElements);
			code.add(Label, endFindValidArrayElements);
			
			
			//creating the new array
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(PushI, ArrayType.headerSize());
			code.add(PushI, storeType.getSize());
			code.add(PushD, RunTime.GLOBAL_TEMP_INTEGER_1);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			code.add(StoreI);
			
			int memLocation = 0;
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, 7);
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			if (node.getType() instanceof ArrayType && ((ArrayType)node.getType()).getSubType() instanceof ArrayType) {
				code.add(PushI, 2);
			} else {
				code.add(PushI, 0);
			}
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, ((ArrayType)node.getType()).getSubtypeSize());
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_TEMP_INTEGER_1);
			code.add(LoadI);
			code.add(StoreI);
			memLocation += 4;
			
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(PushI, 0);
			code.add(StoreI);
			code.add(PushD, RunTime.GLOBAL_COUNTER_2);
			code.add(PushI, 0);
			code.add(StoreI);
			
			//start
			code.add(Label, startReducing);
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.REDUCE_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(Subtract);
			code.add(JumpFalse, endReducing);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_3);
			code.add(LoadI);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(Add);
			code.add(LoadC);
			code.add(PushI, 1);
			code.add(Subtract);
			code.add(JumpFalse, storeValidElement);
			
			code.add(Jump, storeNotValidElement);
			code.add(Label, storeValidElement);
			//body
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_COUNTER_2);
			code.add(LoadI);
			code.add(PushI, storeType.getSize());
			code.add(Multiply);
			code.add(Add);
			code.add(StoreI);
			
			//Loading
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.REDUCE_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.REDUCE_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);
			if (loadType == PrimitiveType.RATIONAL) {
				code.add(Duplicate);
				code.add(opcodeForLoad(loadType));
				code.add(Exchange);
				code.add(PushI, 4);
				code.add(Add);
				code.add(opcodeForLoad(loadType));
				code.add(Exchange);
			} else {
				code.add(opcodeForLoad(loadType));
			}
			//Storing
			if (storeType == PrimitiveType.RATIONAL) {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(Exchange);
				code.add(storeCode);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(PushI, 4);
				code.add(Add);
				code.add(Exchange);
			} else {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(Exchange);
			}
			code.add(storeCode);
			code.add(PushD, RunTime.GLOBAL_COUNTER_2);
			code.add(PushD, RunTime.GLOBAL_COUNTER_2);
			code.add(LoadI);
			code.add(PushI, 1);
			code.add(Add);
			code.add(StoreI);
			code.add(Label, storeNotValidElement);
			
			//increment
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(Duplicate);
			code.add(LoadI);
			code.add(PushI, 1);
			code.add(Add);
			code.add(StoreI);
			code.add(Jump, startReducing);
			code.add(Label, endReducing);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_3);
			code.add(LoadI);
			code.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);
		}
		
		public void visitLeave(MapStatementNode node) {
			newValueCode(node);
			ASMCodeFragment arrayExpression = removeValueCode(node.child(0));
			ASMCodeFragment lambdaExpression = removeValueCode(node.child(1));
			
			Labeller mapLabeller = new Labeller("map-statement");
			String startMapping = mapLabeller.newLabel("start");
			String endMapping = mapLabeller.newLabel("end");
			
			Type storeType = ((ArrayType)node.getType()).getSubType();
			Type loadType = ((ArrayType)node.child(0).getType()).getSubType();
			ASMOpcode storeCode = opcodeForStore(storeType);
			
			code.add(PushD, RunTime.MAP_ARRAY_EXPRESSION_POINTER);
			code.append(arrayExpression.makeCopy());
			code.add(StoreI);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(PushI, ArrayType.headerSize());
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.MAP_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(PushI, storeType.getSize());
			code.add(Multiply);
			code.add(Add);
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			code.add(StoreI);
			
			int memLocation = 0;
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, 7);
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			if (node.getType() instanceof ArrayType && ((ArrayType)node.getType()).getSubType() instanceof ArrayType) {
				code.add(PushI, 2);
			} else {
				code.add(PushI, 0);
			}
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, ((ArrayType)node.getType()).getSubtypeSize());
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.MAP_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(StoreI);
			memLocation += 4;
			
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(PushI, 0);
			code.add(StoreI);
			
			//start
			code.add(Label, startMapping);
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.MAP_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(Subtract);
			code.add(JumpFalse, endMapping);
			
			//body
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(PushI, storeType.getSize());
			code.add(Multiply);
			code.add(Add);
			code.add(StoreI);
			
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.MAP_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			//code.append(arrayExpression.makeCopy());
			code.add(PushD, RunTime.MAP_ARRAY_EXPRESSION_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);
			if (loadType == PrimitiveType.RATIONAL) {
				code.add(Duplicate);
				code.add(opcodeForLoad(loadType));
				code.add(Exchange);
				code.add(PushI, 4);
				code.add(Add);
				code.add(opcodeForLoad(loadType));
				code.add(Exchange);
			} else {
				code.add(opcodeForLoad(loadType));
			}
			code.append(lambdaExpression.makeCopy());
			code.add(CallV);
			if (storeType == PrimitiveType.RATIONAL) {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(Exchange);
				code.add(storeCode);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(PushI, 4);
				code.add(Add);
				code.add(Exchange);
			} else {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(Exchange);
			}
			code.add(storeCode);
			
			//increment
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(Duplicate);
			code.add(LoadI);
			code.add(PushI, 1);
			code.add(Add);
			code.add(StoreI);
			code.add(Jump, startMapping);
			code.add(Label, endMapping);
		}
		
		public void visitLeave(ZipStatementNode node) {
			newValueCode(node);
			ASMCodeFragment arrayExpression1 = removeValueCode(node.child(0));
			ASMCodeFragment arrayExpression2 = removeValueCode(node.child(1));
			ASMCodeFragment lambdaExpression = removeValueCode(node.child(2));
			
			Labeller zipLabeller = new Labeller("zip-statement");
			String startZip = zipLabeller.newLabel("start");
			String endZip = zipLabeller.newLabel("end");
			
			Type storeType = ((ArrayType)node.getType()).getSubType();
			Type loadType1 = ((ArrayType)node.child(0).getType()).getSubType();
			Type loadType2 = ((ArrayType)node.child(1).getType()).getSubType();
			ASMOpcode storeCode = opcodeForStore(storeType);
			
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_1);
			code.append(arrayExpression1.makeCopy());
			code.add(StoreI);
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_2);
			code.append(arrayExpression2.makeCopy());
			code.add(StoreI);
			
			//code.append(arrayExpression1.makeCopy());
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			//code.append(arrayExpression2.makeCopy());
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_2);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Subtract);
			code.add(JumpTrue, RunTime.ZIP_ARRAY_LENGTH_ERROR);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(PushI, ArrayType.headerSize());
			//code.append(arrayExpression1.makeCopy());
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(PushI, storeType.getSize());
			code.add(Multiply);
			code.add(Add);
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			code.add(StoreI);
			
			int memLocation = 0;
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, 7);
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			if (node.getType() instanceof ArrayType && ((ArrayType)node.getType()).getSubType() instanceof ArrayType) {
				code.add(PushI, 2);
			} else {
				code.add(PushI, 0);
			}
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, ((ArrayType)node.getType()).getSubtypeSize());
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			//code.append(arrayExpression1.makeCopy());
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(StoreI);
			
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(PushI, 0);
			code.add(StoreI);
			
			//start
			code.add(Label, startZip);
			//code.append(arrayExpression1.makeCopy());
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(Subtract);
			code.add(JumpFalse, endZip);
			
			//body
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(PushI, storeType.getSize());
			code.add(Multiply);
			code.add(Add);
			code.add(StoreI);
			//Argument 1 for Lambda
			//code.append(arrayExpression1.makeCopy());
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			//code.append(arrayExpression1.makeCopy());
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_1);
			code.add(LoadI);
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);
			if (loadType1 == PrimitiveType.RATIONAL) {
				code.add(Duplicate);
				code.add(opcodeForLoad(loadType1));
				code.add(Exchange);
				code.add(PushI, 4);
				code.add(Add);
				code.add(opcodeForLoad(loadType1));
				code.add(Exchange);
			} else {
				code.add(opcodeForLoad(loadType1));
			}
			//Argument 2 for Lambda
			//code.append(arrayExpression2.makeCopy());
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_2);
			code.add(LoadI);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			//code.append(arrayExpression2.makeCopy());
			code.add(PushD, RunTime.ZIP_ARRAY_EXPRESSION_POINTER_2);
			code.add(LoadI);
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);
			if (loadType2 == PrimitiveType.RATIONAL) {
				code.add(Duplicate);
				code.add(opcodeForLoad(loadType2));
				code.add(Exchange);
				code.add(PushI, 4);
				code.add(Add);
				code.add(opcodeForLoad(loadType2));
				code.add(Exchange);
			} else {
				code.add(opcodeForLoad(loadType2));
			}
			code.append(lambdaExpression.makeCopy());
			code.add(CallV);
			if (storeType == PrimitiveType.RATIONAL) {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(Exchange);
				code.add(storeCode);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(PushI, 4);
				code.add(Add);
				code.add(Exchange);
			} else {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				code.add(Exchange);
			}
			code.add(storeCode);
			
			//increment
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(Duplicate);
			code.add(LoadI);
			code.add(PushI, 1);
			code.add(Add);
			code.add(StoreI);
			code.add(Jump, startZip);
			code.add(Label, endZip);
		}
		
		public void visitLeave(FoldStatementNode node) {
			newValueCode(node);
			if (node.nChildren() == 2) {
				ASMCodeFragment arrayExpression = removeValueCode(node.child(0));
				ASMCodeFragment lambdaExpression = removeValueCode(node.child(1));
				
				Labeller foldLabeller = new Labeller("fold-statement");
				String arrayLengthOne = foldLabeller.newLabel("length-is-one");
				String startFoldLoop = foldLabeller.newLabel("start-loop");
				String endFoldLoop = foldLabeller.newLabel("end-loop");
				String endFold = foldLabeller.newLabel("end");
				
				Type storeType = node.getType();
				Type loadType = ((ArrayType)node.child(0).getType()).getSubType();
				ASMOpcode storeCode = opcodeForStore(storeType);
				
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.append(arrayExpression.makeCopy());
				code.add(StoreI);
				
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(JumpFalse, RunTime.FOLD_ARRAY_LENGTH_ERROR);
				
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Subtract);
				code.add(JumpFalse, arrayLengthOne);
				
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(PushI, loadType.getSize());
				code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
				code.add(StoreI);
				
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.headerSize());
				code.add(Add);
				if (loadType == PrimitiveType.RATIONAL) {
					code.add(Duplicate);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
					code.add(PushI, 4);
					code.add(Add);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
				} else {
					code.add(opcodeForLoad(loadType));
				}
				if (storeType == PrimitiveType.RATIONAL) {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(Exchange);
					code.add(storeCode);
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(PushI, 4);
					code.add(Add);
					code.add(Exchange);
				} else {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(Exchange);
				}
				code.add(storeCode);
				
				//initializing the counter
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(PushI, 1);
				code.add(StoreI);
				
				//start of the loop
				code.add(Label, startFoldLoop);
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(LoadI);
				code.add(Subtract);
				code.add(JumpFalse, endFoldLoop);
				
				//Argument 1 for Lambda
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				if (loadType == PrimitiveType.RATIONAL) {
					code.add(Duplicate);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
					code.add(PushI, 4);
					code.add(Add);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
				} else {
					code.add(opcodeForLoad(loadType));
				}
				//Argument 2 for Lambda
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.headerSize());
				code.add(Add);
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(LoadI);
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.subtypeSizeOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(Multiply);
				code.add(Add);
				if (loadType == PrimitiveType.RATIONAL) {
					code.add(Duplicate);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
					code.add(PushI, 4);
					code.add(Add);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
				} else {
					code.add(opcodeForLoad(loadType));
				}
				code.append(lambdaExpression.makeCopy());
				code.add(CallV);
				//Storing the result of the lambda
				if (storeType == PrimitiveType.RATIONAL) {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(Exchange);
					code.add(storeCode);
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(PushI, 4);
					code.add(Add);
					code.add(Exchange);
				} else {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(Exchange);
				}
				code.add(storeCode);
				
				//increment
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(Duplicate);
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Add);
				code.add(StoreI);
				code.add(Jump, startFoldLoop);
				code.add(Label, endFoldLoop);
				
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				//loading the return value for the fold
				if (loadType == PrimitiveType.RATIONAL) {
					code.add(Duplicate);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
					code.add(PushI, 4);
					code.add(Add);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
				} else {
					code.add(opcodeForLoad(loadType));
				}
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI); // deallocating the temp memory
				code.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);
				
				code.add(Jump, endFold);
				code.add(Label, arrayLengthOne);
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.headerSize());
				code.add(Add);
				if (loadType == PrimitiveType.RATIONAL) {
					code.add(Duplicate);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
					code.add(PushI, 4);
					code.add(Add);
					code.add(opcodeForLoad(loadType));
					code.add(Exchange);
				} else {
					code.add(opcodeForLoad(loadType));
				}
				code.add(Label, endFold);
				
			} else if (node.nChildren() == 3) {
				ASMCodeFragment arrayExpression = removeValueCode(node.child(0));
				ASMCodeFragment baseValue = removeValueCode(node.child(1));
				ASMCodeFragment lambdaExpression = removeValueCode(node.child(2));
				
				Labeller foldLabeller = new Labeller("fold-statement");
				String arrayLengthZero = foldLabeller.newLabel("length-is-zero");
				String startFoldLoop = foldLabeller.newLabel("start-loop");
				String endFoldLoop = foldLabeller.newLabel("end-loop");
				String endFold = foldLabeller.newLabel("end");
				
				Type storeType = node.getType();
				Type loadType1 = ((ArrayType)node.child(0).getType()).getSubType();
				Type loadType2 = node.child(1).getType();
				ASMOpcode storeCode = opcodeForStore(storeType);
				
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.append(arrayExpression.makeCopy());
				code.add(StoreI);
				
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(JumpFalse, arrayLengthZero);
				
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(PushI, loadType2.getSize());
				code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
				code.add(StoreI);
				
				code.append(baseValue.makeCopy());
				if (storeType == PrimitiveType.RATIONAL) {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(Exchange);
					code.add(storeCode);
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(PushI, 4);
					code.add(Add);
					code.add(Exchange);
				} else {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(Exchange);
				}
				code.add(storeCode);
				
				//initializing the counter
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(PushI, 0);
				code.add(StoreI);
				
				//start of the loop
				code.add(Label, startFoldLoop);
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(LoadI);
				code.add(Subtract);
				code.add(JumpFalse, endFoldLoop);
				
				//Argument 1 for Lambda
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				if (loadType2 == PrimitiveType.RATIONAL) {
					code.add(Duplicate);
					code.add(opcodeForLoad(loadType2));
					code.add(Exchange);
					code.add(PushI, 4);
					code.add(Add);
					code.add(opcodeForLoad(loadType2));
					code.add(Exchange);
				} else {
					code.add(opcodeForLoad(loadType2));
				}
				//Argument 2 for Lambda
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.headerSize());
				code.add(Add);
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(LoadI);
				//code.append(arrayExpression.makeCopy());
				code.add(PushD, RunTime.FOLD_ARRAY_EXPRESSION_POINTER);
				code.add(LoadI);
				code.add(PushI, ArrayType.subtypeSizeOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(Multiply);
				code.add(Add);
				if (loadType1 == PrimitiveType.RATIONAL) {
					code.add(Duplicate);
					code.add(opcodeForLoad(loadType1));
					code.add(Exchange);
					code.add(PushI, 4);
					code.add(Add);
					code.add(opcodeForLoad(loadType1));
					code.add(Exchange);
				} else {
					code.add(opcodeForLoad(loadType1));
				}
				code.append(lambdaExpression.makeCopy());
				code.add(CallV);
				//Storing the result of the lambda
				if (storeType == PrimitiveType.RATIONAL) {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(Exchange);
					code.add(storeCode);
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(PushI, 4);
					code.add(Add);
					code.add(Exchange);
				} else {
					code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
					code.add(LoadI);
					code.add(Exchange);
				}
				code.add(storeCode);
				
				//increment
				code.add(PushD, RunTime.GLOBAL_COUNTER);
				code.add(Duplicate);
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Add);
				code.add(StoreI);
				code.add(Jump, startFoldLoop);
				code.add(Label, endFoldLoop);
				
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				//loading the return value for the fold
				if (loadType2 == PrimitiveType.RATIONAL) {
					code.add(Duplicate);
					code.add(opcodeForLoad(loadType2));
					code.add(Exchange);
					code.add(PushI, 4);
					code.add(Add);
					code.add(opcodeForLoad(loadType2));
					code.add(Exchange);
				} else {
					code.add(opcodeForLoad(loadType2));
				}
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI); // deallocating the temp memory
				code.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);
				
				code.add(Jump, endFold);
				code.add(Label, arrayLengthZero);
				code.append(baseValue.makeCopy());
				code.add(Label, endFold);
			}
		}
		
		public void visitLeave(BinaryOperatorNode node) {
			Lextant operator = node.getOperator();

			if(operator == Punctuator.GREATER || 
					operator == Punctuator.GREATER_EQUAL || 
					operator == Punctuator.LESSER ||
					operator == Punctuator.LESSER_EQUAL || 
					operator == Punctuator.EQUAL ||
					operator == Punctuator.NOT_EQUAL) {
				visitComparisonOperatorNode(node, operator);
			} else if (operator == Punctuator.AND) {
				visitAndOperatorNode(node);
			} else if (operator == Punctuator.OR) {
				visitOrOperatorNode(node);
			} else if (operator == Punctuator.OVER) {
				visitOverBinaryOperatorNode(node);
			} else if (operator == Punctuator.EXPRESS_OVER) {
				visitExpressOverBinaryOperatorNode(node);
			} else if (operator == Punctuator.RATIONALIZE) {
				visitRationalizeBinaryOperatorNode(node);
			}
			else {
				if (node.getType() == PrimitiveType.RATIONAL) {
					visitRationalBinaryOperatorNode(node);
				} else if (node.getType() == PrimitiveType.STRING) {
					visitStringBinaryOperatorNode(node);
				} else {
					visitNormalBinaryOperatorNode(node);
				}
			}
		}
		
		public void visitLeave(IndexOperatorNode node) {
			newAddressCode(node);
			ASMCodeFragment target = removeValueCode(node.child(0));
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1); //Target
			code.append(target.makeCopy());
			code.add(StoreI);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			Type targetType = node.child(0).getType();
			for (int i = 1; i < node.nChildren(); i++) {
				ASMCodeFragment index = removeValueCode(node.child(i));
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2); //Index
				code.append(index.makeCopy());
				if (node.child(i).getType() instanceof PrimitiveType && !(node.child(i).getType().equals(PrimitiveType.INTEGER))) {
					generateCastOpcode(PrimitiveType.INTEGER, node.child(i).getType());
				}
				code.add(StoreI);
				
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				if (targetType instanceof ArrayType) {
					code.add(PushI, ArrayType.lengthOffset());
				} else if (targetType == PrimitiveType.STRING) {
					code.add(PushI, PrimitiveType.STRING.lengthOffset());
				}
				code.add(Add);
				code.add(LoadI);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				generateTestIndexOutOfBounds();
				
				if (targetType instanceof ArrayType) {
					code.add(PushI, ArrayType.headerSize());
					code.add(Add);
				} else if (targetType == PrimitiveType.STRING) {
					code.add(PushI, PrimitiveType.STRING.getHeaderSize());
					code.add(Add);
				}
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.add(LoadI);
				if (targetType instanceof ArrayType) {
					targetType = ((ArrayType)targetType).getSubType();
				} else {
					targetType = node.getType();
				}
				code.add(PushI, targetType.getSize());
				code.add(Multiply);
				code.add(Add);
				
				ASMOpcode loadCode = opcodeForLoad(targetType);
				
				if (i < node.nChildren() - 1) {
					code.add(loadCode);
				}
				
				
			}
		}
		
		public void visitLeave(SubstringStatementNode node) {
			newValueCode(node);
			ASMCodeFragment sourceString = removeValueCode(node.child(0));
			ASMCodeFragment startingIndex = removeValueCode(node.child(1));
			ASMCodeFragment endingIndex = removeValueCode(node.child(2));
			
			
			code.append(sourceString.makeCopy());
			code.add(PushI, PrimitiveType.STRING.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.append(startingIndex.makeCopy());
			code.add(Subtract);
			code.add(JumpNeg, RunTime.INDEX_OUT_OF_BOUNDS_ERROR);
			code.append(startingIndex.makeCopy());
			code.add(JumpNeg, RunTime.INDEX_OUT_OF_BOUNDS_ERROR);
			
			code.append(sourceString.makeCopy());
			code.add(PushI, PrimitiveType.STRING.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.append(endingIndex.makeCopy());
			code.add(Subtract);
			code.add(JumpNeg, RunTime.INDEX_OUT_OF_BOUNDS_ERROR);
			code.append(endingIndex.makeCopy());
			code.add(JumpNeg, RunTime.INDEX_OUT_OF_BOUNDS_ERROR);
			
			code.append(endingIndex.makeCopy());
			code.append(startingIndex.makeCopy());
			code.add(Subtract);
			code.add(Duplicate);
			code.add(JumpNeg, RunTime.INDEX_OUT_OF_BOUNDS_ERROR);
			code.add(JumpFalse, RunTime.INDEX_OUT_OF_BOUNDS_ERROR);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(PushI, PrimitiveType.STRING.getHeaderSize());
			code.append(endingIndex.makeCopy());
			code.append(startingIndex.makeCopy());
			code.add(Subtract);
			code.add(Add);
			code.add(PushI, 1);
			code.add(Add);
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			code.add(StoreI);
			
			//setting up new string
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, 6);
			code.add(StoreI); //storing header info
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, 4);
			code.add(Add);
			code.add(PushI, 9);
			code.add(StoreI); //storing header info
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, 8);
			code.add(Add);
			code.append(endingIndex.makeCopy());
			code.append(startingIndex.makeCopy());
			code.add(Subtract);
			code.add(StoreI); //storing length
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(Duplicate);
			code.add(PushI, PrimitiveType.STRING.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Add);
			code.add(PushI, PrimitiveType.STRING.getHeaderSize());
			code.add(Add);
			code.add(PushI, 0);
			code.add(StoreC); //storing ending 0 character
			
			//copying bytes
			code.append(sourceString.makeCopy());
			code.add(PushI, PrimitiveType.STRING.getHeaderSize());
			code.add(Add);
			code.append(startingIndex.makeCopy());
			code.add(Add);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, PrimitiveType.STRING.getHeaderSize());
			code.add(Add);
			
			code.append(endingIndex.makeCopy());
			code.append(startingIndex.makeCopy());
			code.add(Subtract);
			
			code.add(Call, SubRoutines.COPY_BYTES_SUBROUTINE);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
		}
		
		public void visitAndOperatorNode(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
				
			Labeller labeller = new Labeller("compare");
			
			String arg1Label = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, arg1Label);
			code.append(arg1.makeCopy());
			code.add(JumpFalse, falseLabel);
			code.add(Label, arg2Label);
			code.append(arg2.makeCopy());
			code.add(JumpFalse, falseLabel);
			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		public void visitOrOperatorNode(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
				
			Labeller labeller = new Labeller("compare");
			
			String arg1Label = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, arg1Label);
			code.append(arg1.makeCopy());
			code.add(JumpTrue, trueLabel);
			code.add(Label, arg2Label);
			code.append(arg2.makeCopy());
			code.add(JumpTrue, trueLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		public void visitLeave(NotOperatorNode node) {
			ASMCodeFragment arg = removeValueCode(node.child(0));
				
			Labeller labeller = new Labeller("not-operator");
			
			String arg1Label = labeller.newLabel("arg");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, arg1Label);
			code.append(arg.makeCopy());
			code.add(JumpTrue, falseLabel);
			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		private void visitComparisonOperatorNode(BinaryOperatorNode node,
				Lextant operator) {
			if (operator == Punctuator.GREATER) {
				generateGreaterComparison(node);
			} else if (operator == Punctuator.GREATER_EQUAL) {
				generateGreaterEqualComparison(node);
			} else if (operator == Punctuator.LESSER) {
				generateLesserComparison(node);
			} else if (operator == Punctuator.LESSER_EQUAL) {
				generateLesserEqualComparison(node);
			} else if (operator == Punctuator.EQUAL) {
				generateEqualComparison(node);
			} else if (operator == Punctuator.NOT_EQUAL) {
				generateNotEqualComparison(node);
			}
		}
		
		public void visitOverBinaryOperatorNode(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			Labeller labeller = new Labeller("over-operator");
			String denominatorIsGCD = labeller.newLabel("skip-gcd-division");
			newValueCode(node);
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.append(arg1.makeCopy());
			code.add(StoreI);
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.append(arg2.makeCopy());
			code.add(StoreI);
			
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(LoadI);
			code.add(JumpFalse, RunTime.RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);
			
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(LoadI);
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(LoadI);
			code.add(Call, SubRoutines.FIND_GREATEST_COMMON_DENOMINATOR);
			
			code.add(Duplicate);
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(Exchange);
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(LoadI);
			code.add(Exchange);
			code.add(Divide);
			code.add(StoreI);
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(Exchange);
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(LoadI);
			code.add(Exchange);
			code.add(Divide);
			code.add(StoreI);
			
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(LoadI);
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(LoadI);
		}
		
		public void visitExpressOverBinaryOperatorNode(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			Type numeratorType = node.child(0).getType();
			Labeller labeller = new Labeller("express-over-operator");
			String denominatorIsGCD = labeller.newLabel("skip-gcd-division");
			newValueCode(node);
			
			code.append(arg1.makeCopy());
			if (numeratorType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, numeratorType);
			}
			code.append(arg2.makeCopy());
			code.add(Duplicate);
			code.add(JumpFalse, RunTime.RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);
			code.add(ConvertF);
			code.add(FMultiply);
			generateCastOpcode(PrimitiveType.INTEGER, PrimitiveType.FLOAT);
		}
		
		public void visitRationalizeBinaryOperatorNode(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			Type numeratorType = node.child(0).getType();
			newValueCode(node);
			code.append(arg1.makeCopy());
			if (numeratorType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, numeratorType);
			}
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.append(arg2.makeCopy());
			code.add(StoreI);
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(LoadI);
			code.add(Duplicate);
			code.add(JumpFalse, RunTime.RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);
			code.add(ConvertF);
			code.add(FMultiply);
			generateCastOpcode(PrimitiveType.INTEGER, PrimitiveType.FLOAT);
			
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(Exchange);
			code.add(StoreI);
			
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(LoadI);
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(LoadI);
			code.add(Call, SubRoutines.FIND_GREATEST_COMMON_DENOMINATOR);
			
			code.add(Duplicate);
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(Exchange);
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(LoadI);
			code.add(Exchange);
			code.add(Divide);
			code.add(StoreI);
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(Exchange);
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(LoadI);
			code.add(Exchange);
			code.add(Divide);
			code.add(StoreI);
			
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(LoadI);
			code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(LoadI);
		}
		
		private void visitRationalBinaryOperatorNode(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			ASMOpcode opcode = opcodeForOperator(node.getOperator(), node.getType());
			newValueCode(node);
			
			code.append(arg1.makeCopy());
			//code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
			code.add(Exchange);
			code.add(StoreI);
			//code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
			code.add(Exchange);
			code.add(StoreI);
			
			code.append(arg2.makeCopy());
			//code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_2);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_2);
			code.add(Exchange);
			code.add(StoreI);
			//code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_2);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_2);
			code.add(Exchange);
			code.add(StoreI);
			if (node.getOperator() == Punctuator.ADD || node.getOperator() == Punctuator.SUBTRACT) {
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
				code.add(LoadI);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_2);
				code.add(LoadI);
				code.add(Multiply);
				code.add(StoreI);
				
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_2);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_2);
				code.add(LoadI);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
				code.add(LoadI);
				code.add(Multiply);
				code.add(StoreI);
				
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
				code.add(LoadI);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_2);
				code.add(LoadI);
				code.add(Multiply);
				code.add(StoreI);
				
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
				code.add(LoadI);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_2);
				code.add(LoadI);
				code.add(opcode);
				code.add(StoreI);
			} else if (node.getOperator() == Punctuator.MULTIPLY) {
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
				code.add(LoadI);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_2);
				code.add(LoadI);
				code.add(Multiply);
				code.add(StoreI);
				
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
				code.add(LoadI);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_2);
				code.add(LoadI);
				code.add(Multiply);
				code.add(StoreI);
			} else if (node.getOperator() == Punctuator.DIVIDE) {
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
				code.add(LoadI);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_2);
				code.add(LoadI);
				code.add(Multiply);
				code.add(StoreI);
				
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
				code.add(LoadI);
				code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_2);
				code.add(LoadI);
				code.add(Multiply);
				code.add(StoreI);
			}
			code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
			code.add(LoadI);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
			code.add(LoadI);
			code.add(Call, SubRoutines.FIND_GREATEST_COMMON_DENOMINATOR);
			
			code.add(Duplicate);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
			code.add(Exchange);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
			code.add(LoadI);
			code.add(Exchange);
			code.add(Divide);
			code.add(StoreI);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
			code.add(Exchange);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
			code.add(LoadI);
			code.add(Exchange);
			code.add(Divide);
			code.add(StoreI);
			
			
			code.add(PushD, RunTime.RATIONAL_OPERATOR_DENOMINATOR_1);
			code.add(LoadI);
			code.add(PushD, RunTime.RATIONAL_OPERATOR_NUMERATOR_1);
			code.add(LoadI);
		}
		
		private void visitStringBinaryOperatorNode(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			ParseNode child1 = node.child(0);
			ParseNode child2 = node.child(1);
			
			Type nodeType = node.getType();
			
			newValueCode(node);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(PushI, PrimitiveType.STRING.getHeaderSize()); //setting up header size for new string
			if (child1.getType() == PrimitiveType.STRING) {
				code.append(arg1.makeCopy());
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI); //length of string 1
			} else {
				code.add(PushI, 1); //1 byte length if char type
			}
			code.add(Add); //Adding length of the first argument
			if (child2.getType() == PrimitiveType.STRING) {
				code.append(arg2.makeCopy());
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI); //length of string 2
			} else {
				code.add(PushI, 1); //1 byte length if char type
			}
			code.add(Add); //Adding length of the second argument
			code.add(PushI, 1);
			code.add(Add); //final byte for the 0 terminating character
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE); //allocating new string
			code.add(StoreI);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, 6);
			code.add(StoreI);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, 4);
			code.add(Add);
			code.add(PushI, 9);
			code.add(StoreI);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, 8);
			code.add(Add);
			if (child1.getType() == PrimitiveType.STRING) {
				code.append(arg1.makeCopy());
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI); //length of string 1
			} else {
				code.add(PushI, 1); //1 byte length if char type
			}
			if (child2.getType() == PrimitiveType.STRING) {
				code.append(arg2.makeCopy());
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI); //length of string 2
			} else {
				code.add(PushI, 1); //1 byte length if char type
			}
			code.add(Add); //Adding length of the second argument
			code.add(StoreI); //storing length of arg1 + length of arg2
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, PrimitiveType.STRING.getHeaderSize());
			code.add(Add);
			if (child1.getType() == PrimitiveType.STRING) { //off-setting the new strings pointer by the length of arg1
				code.append(arg1.makeCopy());
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI); //length of string 1
			} else {
				code.add(PushI, 1); //1 byte length if char type
			}
			code.add(Add);
			if (child2.getType() == PrimitiveType.STRING) {
				code.append(arg2.makeCopy());
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI); //length of string 2
			} else {
				code.add(PushI, 1); //1 byte length if char type
			}
			code.add(Add);
			code.add(PushI, 0);
			code.add(StoreC);
			
			//Copying arg1 to the new string
			if (child1.getType() == PrimitiveType.STRING) {
				code.append(arg1.makeCopy());
				code.add(PushI, PrimitiveType.STRING.getHeaderSize());
				code.add(Add);
			} else {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.append(arg1.makeCopy());
				code.add(StoreI);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
			}
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, PrimitiveType.STRING.getHeaderSize());
			code.add(Add);
			if (child1.getType() == PrimitiveType.STRING) {
				code.append(arg1.makeCopy());
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI); //length of string 1
			} else {
				code.add(PushI, 1); //1 byte length if char type
			}
			code.add(Call, SubRoutines.COPY_BYTES_SUBROUTINE);
			
			//Copying arg2 to the new string
			
			if (child2.getType() == PrimitiveType.STRING) {
				code.append(arg2.makeCopy());
				code.add(PushI, PrimitiveType.STRING.getHeaderSize());
				code.add(Add);
			} else {
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
				code.append(arg2.makeCopy());
				code.add(StoreI);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_2);
			}
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
			code.add(PushI, PrimitiveType.STRING.getHeaderSize());
			code.add(Add);
			if (child1.getType() == PrimitiveType.STRING) { //off-setting the new strings pointer by the length of arg1
				code.append(arg1.makeCopy());
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI); //length of string 1
			} else {
				code.add(PushI, 1); //1 byte length if char type
			}
			code.add(Add);
			if (child2.getType() == PrimitiveType.STRING) {
				code.append(arg2.makeCopy());
				code.add(PushI, PrimitiveType.STRING.lengthOffset());
				code.add(Add);
				code.add(LoadI); //length of string 2
			} else {
				code.add(PushI, 1); //1 byte length if char type
			}
			code.add(Call, SubRoutines.COPY_BYTES_SUBROUTINE);
			
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(LoadI);
		}
		
		private void visitNormalBinaryOperatorNode(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			ParseNode child1 = node.child(0);
			ParseNode child2 = node.child(1);
			
			Type nodeType = node.getType();
			
			newValueCode(node);
			code.append(arg1.makeCopy());
			if (child1.getType() instanceof PrimitiveType && !(child1.getType().equals(nodeType))) {
				generateCastOpcode(nodeType, child1.getType());
			}
			code.append(arg2.makeCopy());
			if (child2.getType() instanceof PrimitiveType && !(child2.getType().equals(nodeType))) {
				generateCastOpcode(nodeType, child2.getType());
			}
			if (node.getOperator() == Punctuator.DIVIDE) {
				generateTestDivideByZero(nodeType);
			}
			
			ASMOpcode opcode = opcodeForOperator(node.getOperator(), nodeType);
			code.add(opcode);							// type-dependent! (opcode is different for floats and for ints)
		}
		private ASMOpcode opcodeForOperator(Lextant lextant, Type type) {
			assert(lextant instanceof Punctuator);
			assert(type instanceof PrimitiveType);
			Punctuator punctuator = (Punctuator)lextant;
			PrimitiveType primitive = (PrimitiveType)type;
			switch(primitive) {
			case INTEGER:
				switch(punctuator) {
				case ADD: 	   		return Add;				// type-dependent!
				case SUBTRACT:      return Subtract;        // type-dependent!
				case MULTIPLY: 		return Multiply;		// type-dependent!
				case DIVIDE:        return Divide;          // type-dependent!
				default:
					assert false : "unimplemented operator in opcodeForOperator";
				}
			case FLOAT:
				switch(punctuator) {
				case ADD: 	   		return FAdd;			// type-dependent!
				case SUBTRACT:      return FSubtract;       // type-dependent!
				case MULTIPLY: 		return FMultiply;		// type-dependent!
				case DIVIDE:        return FDivide;         // type-dependent!
				default:
					assert false : "unimplemented operator in opcodeForOperator";
				}
			case RATIONAL:
				switch(punctuator) {
				case ADD: 	   		return Add;				// type-dependent!
				case SUBTRACT:      return Subtract;        // type-dependent!
				case MULTIPLY: 		return Multiply;		// type-dependent!
				case DIVIDE:        return Divide;          // type-dependent!
				default:
					assert false : "unimplemented operator in opcodeForOperator";
				}
			default:
				assert false : "unimplemented primitive type in opcodeForOperator";
			}
			return null;
		}
		
		public void visitLeave(ArrayExpressionNode node) {
			newValueCode(node);
			//Labeller labeller = new Labeller("array");
			//String dlabel = labeller.newLabel("");
			//code.add(DLabel, dlabel);
			//code.add(DataI, 7);
			//code.add(DataI, 2);
			//code.add(DataI, node.getType().getSize());
			//code.add(DataI, node.nChildren());
			//for (ParseNode child : node.getChildren()) {
				//code.append(removeValueCode(child));
			//}
			//code.add(PushD, dlabel);
			Type subType = null;
			if (node.getType() instanceof ArrayType) {
				subType = ((ArrayType)node.getType()).getSubType();
			}
			ASMCodeFragment length1 = null;
			ASMCodeFragment length2 = null;
			if (node.child(0) instanceof TypeLiteralNode) {
				length1 = removeValueCode(node.child(1));
				length2 = length1.makeCopy();
				code.append(length1);
				code.add(Duplicate);
				code.add(JumpNeg, RunTime.NEGATIVE_LENGTH_FOR_ARRAY_ERROR);
			} else {
				code.add(PushI, node.nChildren());
			}
			code.add(PushI, ((ArrayType)node.getType()).getSubtypeSize());
			code.add(Multiply);
			code.add(PushI, node.getTypeHeaderSize());
			code.add(Add);
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			
			int memLocation = 0;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, 7);
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			if (node.getType() instanceof ArrayType && ((ArrayType)node.getType()).getSubType() instanceof ArrayType) {
				code.add(PushI, 2);
			} else {
				code.add(PushI, 0);
			}
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, ((ArrayType)node.getType()).getSubtypeSize());
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			if (node.child(0) instanceof TypeLiteralNode && length2 != null) {
				code.append(length2);
			} else {
				code.add(PushI, node.nChildren());
			}
			code.add(StoreI);
			memLocation += 4;
			if (!(node.child(0) instanceof TypeLiteralNode)) {
				for (ParseNode child : node.getChildren()) {
					ASMOpcode storeOpcode = opcodeForStore(subType);
					code.add(Duplicate);
					code.add(PushI, memLocation);
					code.add(Add);
					if (subType == PrimitiveType.RATIONAL) {
						code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
						code.add(Exchange);
						code.add(StoreI);
					}
					
					code.append(removeValueCode(child).makeCopy());
					
					if (subType == PrimitiveType.RATIONAL) {
						code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
						code.add(LoadI);
						code.add(Exchange);
						code.add(storeOpcode);
						code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
						code.add(LoadI);
						code.add(PushI, 4);
						code.add(Add);
						code.add(Exchange);
					}
					
					if (child.getType() instanceof PrimitiveType && !(child.getType().equals(subType))) {
						generateCastOpcode(subType, child.getType());
					}
					code.add(storeOpcode);
					memLocation += ((ArrayType)node.getType()).getSubtypeSize();
				}
			}
		}
		
		public void visitLeave(CloneStatementNode node) {
			newValueCode(node);
			ASMCodeFragment referenceArray = removeValueCode(node.child(0));
			Type childType = ((ArrayType)node.child(0).getType()).getSubType();
			Labeller cloneLabeller = new Labeller("clone");
			String startRefArray = cloneLabeller.newLabel("start-ref-array");
			String endRefArray = cloneLabeller.newLabel("end-ref-array");
			String start = cloneLabeller.newLabel("start");
			String end = cloneLabeller.newLabel("end");
			String startHeaderClone = cloneLabeller.newLabel("start-header-clone");
			String endHeaderClone = cloneLabeller.newLabel("end-header-clone");
			String startContentClone = cloneLabeller.newLabel("start-content-clone");
			String endContentClone = cloneLabeller.newLabel("end-content-clone");
			
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.append(referenceArray.makeCopy());
			code.add(StoreI);
			
			
			//code.add(Call, startRefArray);
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.add(LoadI);
			
			code.add(Duplicate);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Exchange);
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Exchange);
			code.add(Multiply);
			code.add(PushI, node.getTypeHeaderSize());
			code.add(Add);
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			code.add(Duplicate);
			code.add(Duplicate);
			code.add(Duplicate);
			code.add(Duplicate);
			code.add(Label, start);
			
			code.add(Label, startHeaderClone);
			//code.add(Call, startRefArray);
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.add(LoadI);
			
			code.add(LoadI);
			code.add(StoreI);
			code.add(PushI, ArrayType.statusOffset());
			code.add(Add);
			//code.add(Call, startRefArray);
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.add(LoadI);
			
			code.add(PushI, ArrayType.statusOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(StoreI);
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			//code.add(Call, startRefArray);
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.add(LoadI);
			
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(StoreI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			//code.add(Call, startRefArray);
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.add(LoadI);
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(StoreI);
			code.add(Label, endHeaderClone);
			
			code.add(Label, startContentClone);
			//code.add(Call, startRefArray);
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.add(LoadI);
			
			code.add(PushI, ArrayType.lengthOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(Subtract);
			code.add(JumpFalse, end);
			
			code.add(Duplicate);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			//code.add(Call, startRefArray);
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.add(LoadI);
			
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
			code.add(Exchange);
			code.add(StoreI);
			
			//code.add(Call, startRefArray);
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.add(LoadI);
			
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			//code.add(Call, startRefArray);
			code.add(PushD, RunTime.GLOBAL_CLONE_POINTER);
			code.add(LoadI);
			
			code.add(PushI, ArrayType.subtypeSizeOffset());
			code.add(Add);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);
			code.add(PushI, ArrayType.headerSize());
			code.add(Add);
			
			ASMOpcode store = opcodeForStore(childType);
			ASMOpcode load = opcodeForLoad(childType);
			if (childType == PrimitiveType.RATIONAL) {
				code.add(Duplicate);
				code.add(load);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(Exchange);
				code.add(store);
				code.add(PushI, 4);
				code.add(Add);
				code.add(load);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(PushI, 4);
				code.add(Add);
				code.add(Exchange);
				code.add(store);
			} else {
				code.add(load);
				code.add(PushD, RunTime.GLOBAL_TEMP_POINTER_1);
				code.add(LoadI);
				code.add(Exchange);
				code.add(store);
			}
			
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(LoadI);
			code.add(PushI, 1);
			code.add(Add);
			code.add(StoreI);
			code.add(Jump, startContentClone);
			code.add(Label, endContentClone);
			
			code.add(Label, end);
			code.add(PushD, RunTime.GLOBAL_COUNTER);
			code.add(PushI, 0);
			code.add(StoreI);
			
			//code.add(Jump, endRefArray);
			//code.add(Label, startRefArray);
			//code.append(referenceArray);
			//code.add(Exchange);
			//code.add(Return);
			code.add(Label, endRefArray);
		}
		
		public void visitLeave(ReleaseStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment referenceVariable = removeValueCode(node.child(0));
			code.append(referenceVariable.makeCopy());
			code.add(Call, SubRoutines.RELEASE_REFERENCE_VARIABLE_SUBROUTINE);
		}
		
		public void visitLeave(ReturnStatementNode node) {
			newVoidCode(node);
			Scope scope = node.getLocalScope();
			int frameSize = scope.getAllocatedSize();
			if (node.nChildren() == 3) {
				ASMCodeFragment returnValue = removeValueCode(node.child(0));
				ASMCodeFragment returnLocation = removeValueCode(node.child(1));
				IdentifierNode dynamicLink = (IdentifierNode) node.child(2);
				code.append(returnValue.makeCopy());
				code.append(returnLocation.makeCopy());
				generateLambdaTeardown(dynamicLink, frameSize);
			} else {
				ASMCodeFragment returnLocation = removeValueCode(node.child(0));
				IdentifierNode dynamicLink = (IdentifierNode) node.child(1);
				code.append(returnLocation.makeCopy());
				generateLambdaTeardown(dynamicLink, frameSize);
			}
			//code.append(returnValue)
			code.add(Return);
		}
		
		public void visitLeave(FunctionDefinitionNode node) {
			newVoidCode(node);
			
			LambdaNode lambdaNode = (LambdaNode) node.child(1);
			Type returnType = lambdaNode.getReturnType();
			ASMCodeFragment identifier = removeAddressCode(node.child(0));
			
			
			//code.add(PushD, lambdaNode.getStartLabel());
			//code.add(StoreI);
			ASMCodeFragment lambdaCode = removeValueCode(lambdaNode);
			code.append(identifier.makeCopy());
			code.append(lambdaCode.makeCopy());
			code.add(opcodeForStore(lambdaNode.getType()));
		}
		
		public void visitLeave(CallStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment functionCall;
			if (node.child(0).getType() instanceof VoidType) {
				functionCall = removeVoidCode(node.child(0));
			} else {
				functionCall = removeValueCode(node.child(0));
			}
			code.append(functionCall.makeCopy());
			if (!(node.child(0).getType() instanceof VoidType)) {
				code.add(Pop);
			}
		}
		
		public void visitLeave(FunctionInvocationNode node) {
			if (node.getType() instanceof VoidType) {
				newVoidCode(node);
			} else {
				newValueCode(node);
			}
			ASMCodeFragment identifier = removeValueCode(node.child(0));
			ASMCodeFragment parameters = removeVoidCode(node.child(1));
			code.append(parameters.makeCopy());
			code.append(identifier.makeCopy());
			code.add(CallV);
		}
		
		public void visitLeave(LambdaNode node) {
			newValueCode(node);
			Labeller labeller = new Labeller("lambda");
			String lambdaStart = labeller.newLabel("start");
			String lambdaEnd = labeller.newLabel("end");
			ASMCodeFragment params = removeVoidCode(node.child(0));
			ASMCodeFragment body = removeVoidCode(node.child(1));
			code.add(Jump, lambdaEnd);
			code.add(Label, lambdaStart);
			code.append(params.makeCopy());
			code.append(body.makeCopy());
			code.add(Jump, RunTime.MISSING_RETURN_STATEMENT_ERROR);
			code.add(Label, lambdaEnd);
			code.add(PushD, lambdaStart);
			node.setStartLabel(lambdaStart);
			node.setEndLabel(lambdaEnd);
		}
		
		public void visitLeave(LambdaParamTypeNode node) {
			newVoidCode(node);
			ASMCodeFragment parameters = removeVoidCode(node.child(0));
			ASMCodeFragment returnAddress = removeAddressCode(node.child(2));
			int frameSize = node.getLocalScope().getAllocatedSize();
			generateLambdaSetup((IdentifierNode)(node.child(3)), frameSize);
			code.append(returnAddress.makeCopy());
			code.add(Exchange);
			code.add(StoreI);
			code.append(parameters.makeCopy());
		}
		
		public void visitLeave(ParameterListNode node) {
			newVoidCode(node);
			for (int i = node.nChildren() - 1; i >= 0; i--) {
				ParseNode child = node.child(i);
				ASMCodeFragment parameter = removeVoidCode(child);
				if (child.getType() == PrimitiveType.RATIONAL) {
					code.append(parameter.makeCopy());
					code.add(Exchange);
					code.add(opcodeForStore(child.getType()));
					code.append(parameter.makeCopy());
					code.add(PushI, 4);
					code.add(Add);
					code.add(Exchange);
				} else {
					code.append(parameter.makeCopy());
					code.add(Exchange);
				}
				code.add(opcodeForStore(child.getType()));
			}
			/*for (ParseNode child : node.getChildren()) {
				ASMCodeFragment parameter = removeVoidCode(child);
				code.append(parameter);
				code.add(Exchange);
				code.add(opcodeForStore(child.getType()));
			}*/
		}
		
		public void visitLeave(ParameterSpecificationNode node) {
			newVoidCode(node);
			//ASMCodeFragment type = removeVoidCode(node.child(0));
			ASMCodeFragment identifier = removeAddressCode(node.child(1));
			code.append(identifier.makeCopy());
		}
		
		public void visitLeave(ExpressionListNode node) {
			newVoidCode(node);
			for (ParseNode child : node.getChildren()) {
				ASMCodeFragment childFrag = removeValueCode(child);
				code.append(childFrag.makeCopy());
			}
		}
		
		public void visitLeave(BreakStatementNode node) {
			newVoidCode(node);
			if (node.getControlFlowParent() != null && node.getControlFlowParent() instanceof WhileStatementNode) {
				WhileStatementNode controlFlowParent = (WhileStatementNode) node.getControlFlowParent();
				code.add(Jump, controlFlowParent.getEndLabel());
			} else if (node.getControlFlowParent() != null && node.getControlFlowParent() instanceof ForStatementNode) {
				ForStatementNode controlFlowParent = (ForStatementNode) node.getControlFlowParent();
				code.add(Jump, controlFlowParent.getEndLabel());
			}
		}
		
		public void visitLeave(ContinueStatementNode node) {
			newVoidCode(node);
			if (node.getControlFlowParent() != null && node.getControlFlowParent() instanceof WhileStatementNode) {
				WhileStatementNode controlFlowParent = (WhileStatementNode) node.getControlFlowParent();
				code.add(Jump, controlFlowParent.getStartLabel());
			} else if (node.getControlFlowParent() != null && node.getControlFlowParent() instanceof ForStatementNode) {
				ForStatementNode controlFlowParent = (ForStatementNode) node.getControlFlowParent();
				code.add(Jump, controlFlowParent.getStartIncrementLabel());
			}
		}

		///////////////////////////////////////////////////////////////////////////
		// leaf nodes (ErrorNode not necessary)
		public void visit(BooleanConstantNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue() ? 1 : 0);
		}
		public void visit(IdentifierNode node) {
			newAddressCode(node);
			Binding binding = node.getBinding();
			
			binding.generateAddress(code);
		}		
		public void visit(IntegerConstantNode node) {
			newValueCode(node);
			
			code.add(PushI, node.getValue());
		}
		public void visit(FloatingConstantNode node) {
			newValueCode(node);
			
			code.add(PushF, node.getValue());
		}
		public void visit(StringConstantNode node) {
			newValueCode(node);
			Labeller labeller = new Labeller("stringConstant");
			String strPointer = labeller.newLabel("");
			code.add(DLabel, strPointer);
			
			//code.add(PushI, node.getTypeHeaderSize() + node.getValue().length() + 1);
			//code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			
			/*int memLocation = 0;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, 6);
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, 7);
			code.add(StoreI);
			memLocation += 4;
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, node.getValue().length());
			code.add(StoreI);
			memLocation += 4;
			for (int i = 0; i < node.getValue().length(); i++) {
				code.add(Duplicate);
				code.add(PushI, memLocation);
				code.add(Add);
				code.add(PushI, node.getValue().charAt(i));
				code.add(StoreI);
				memLocation += 1;
			}
			code.add(Duplicate);
			code.add(PushI, memLocation);
			code.add(Add);
			code.add(PushI, 0);
			code.add(StoreI);*/
			code.add(DataI, 6);
			code.add(DataI, 9);
			code.add(DataI, node.getValue().length());
			code.add(DataS, node.getValue());
			code.add(PushD, strPointer);
			
			
		}
		public void visit(CharacterConstantNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue());
		}
		
		///////////////////////////////////////////////////////////////////////////
		// Comparison operations
		
		private void generateGreaterComparison(BinaryOperatorNode node) {
				
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Type tempType = node.child(0).getType();
			assert(tempType instanceof PrimitiveType);
			PrimitiveType comparisonType = (PrimitiveType)tempType;
				
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1.makeCopy());
			if (comparisonType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
			}
			code.add(Label, arg2Label);
			code.append(arg2.makeCopy());
			if (comparisonType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
			}
			code.add(Label, subLabel);
			if (comparisonType == PrimitiveType.INTEGER || comparisonType == PrimitiveType.CHARACTER) {
				code.add(Subtract);
			} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
				code.add(FSubtract);
			}
			
			if (comparisonType == PrimitiveType.INTEGER || comparisonType == PrimitiveType.CHARACTER) {
				code.add(JumpPos, trueLabel);
			} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
				code.add(JumpFPos, trueLabel);
			}
			code.add(Jump, falseLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		private void generateGreaterEqualComparison(BinaryOperatorNode node) {
			
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Type tempType = node.child(0).getType();
			assert(tempType instanceof PrimitiveType);
			PrimitiveType comparisonType = (PrimitiveType)tempType;
				
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String greaterTrueLabel  = labeller.newLabel("greater-true");
			String equalTrueLabel  = labeller.newLabel("equal-true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1.makeCopy());
			if (comparisonType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
			}
			code.add(Label, arg2Label);
			code.append(arg2.makeCopy());
			if (comparisonType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
			}
			code.add(Label, subLabel);
			if (comparisonType == PrimitiveType.INTEGER || comparisonType == PrimitiveType.CHARACTER) {
				code.add(Subtract);
			} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
				code.add(FSubtract);
			}
			
			code.add(Duplicate);
			if (comparisonType == PrimitiveType.INTEGER || comparisonType == PrimitiveType.CHARACTER) {
				code.add(JumpPos, greaterTrueLabel);
				code.add(JumpFalse, equalTrueLabel);
			} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
				code.add(JumpFPos, greaterTrueLabel);
				code.add(JumpFZero, equalTrueLabel);
			}
			code.add(Jump, falseLabel);

			code.add(Label, greaterTrueLabel);
			code.add(Pop);
			code.add(Label, equalTrueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		private void generateLesserComparison(BinaryOperatorNode node) {
			
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Type tempType = node.child(0).getType();
			assert(tempType instanceof PrimitiveType);
			PrimitiveType comparisonType = (PrimitiveType)tempType;
				
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1.makeCopy());
			if (comparisonType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
			}
			code.add(Label, arg2Label);
			code.append(arg2.makeCopy());
			if (comparisonType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
			}
			code.add(Label, subLabel);
			
			if (comparisonType == PrimitiveType.INTEGER || comparisonType == PrimitiveType.CHARACTER) {
				code.add(Subtract);
			} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
				code.add(FSubtract);
			}
			
			if (comparisonType == PrimitiveType.INTEGER || comparisonType == PrimitiveType.CHARACTER) {
				code.add(JumpNeg, trueLabel);
			} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
				code.add(JumpFNeg, trueLabel);
			}
			//code.add(JumpNeg, trueLabel);JumpFNeg
			code.add(Jump, falseLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		private void generateLesserEqualComparison(BinaryOperatorNode node) {
			
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Type tempType = node.child(0).getType();
			assert(tempType instanceof PrimitiveType);
			PrimitiveType comparisonType = (PrimitiveType)tempType;
				
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String lesserTrueLabel  = labeller.newLabel("lesser-true");
			String equalTrueLabel  = labeller.newLabel("equal-true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1.makeCopy());
			if (comparisonType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
			}
			code.add(Label, arg2Label);
			code.append(arg2.makeCopy());
			if (comparisonType == PrimitiveType.RATIONAL) {
				generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
			}
			code.add(Label, subLabel);
			
			if (comparisonType == PrimitiveType.INTEGER || comparisonType == PrimitiveType.CHARACTER) {
				code.add(Subtract);
			} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
				code.add(FSubtract);
			}
			code.add(Duplicate);
			
			if (comparisonType == PrimitiveType.INTEGER || comparisonType == PrimitiveType.CHARACTER) {
				code.add(JumpNeg, lesserTrueLabel);
				code.add(JumpFalse, equalTrueLabel);
			} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
				code.add(JumpFNeg, lesserTrueLabel);
				code.add(JumpFZero, equalTrueLabel);
			}
			//code.add(JumpNeg, trueLabel);JumpFNeg
			code.add(Jump, falseLabel);

			code.add(Label, lesserTrueLabel);
			code.add(Pop);
			code.add(Label, equalTrueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		private void generateEqualComparison(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			Type tempType = node.child(0).getType();
			if (tempType instanceof PrimitiveType) {
				PrimitiveType comparisonType = (PrimitiveType)tempType;
				
				newValueCode(node);
				code.add(Label, startLabel);
				code.append(arg1.makeCopy());
				if (comparisonType == PrimitiveType.RATIONAL) {
					generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
				}
				code.add(Label, arg2Label);
				code.append(arg2.makeCopy());
				if (comparisonType == PrimitiveType.RATIONAL) {
					generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
				}
				code.add(Label, subLabel);
				if (comparisonType == PrimitiveType.INTEGER 
						|| comparisonType == PrimitiveType.BOOLEAN
						|| comparisonType == PrimitiveType.CHARACTER
						|| comparisonType == PrimitiveType.STRING) {
					code.add(Subtract);
				} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
					code.add(FSubtract);
				}
				
				if (comparisonType == PrimitiveType.INTEGER
						|| comparisonType == PrimitiveType.BOOLEAN
						|| comparisonType == PrimitiveType.CHARACTER
						|| comparisonType == PrimitiveType.STRING) {
					code.add(JumpFalse, trueLabel);
				} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
					code.add(JumpFZero, trueLabel);
				}
			} else {
				Type comparisonType = tempType;
				
				newValueCode(node);
				code.add(Label, startLabel);
				code.append(arg1.makeCopy());
				
				code.add(Label, arg2Label);
				code.append(arg2.makeCopy());
				
				code.add(Label, subLabel);
				if (comparisonType instanceof ArrayType) {
					code.add(Subtract);
				}
				
				if (comparisonType instanceof ArrayType) {
					code.add(JumpFalse, trueLabel);
				}
			}
			code.add(Jump, falseLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		private void generateNotEqualComparison(BinaryOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			Type tempType = node.child(0).getType();
			if (tempType instanceof PrimitiveType) {
				PrimitiveType comparisonType = (PrimitiveType)tempType;
				
				newValueCode(node);
				code.add(Label, startLabel);
				code.append(arg1.makeCopy());
				if (comparisonType == PrimitiveType.RATIONAL) {
					generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
				}
				code.add(Label, arg2Label);
				code.append(arg2.makeCopy());
				if (comparisonType == PrimitiveType.RATIONAL) {
					generateCastOpcode(PrimitiveType.FLOAT, comparisonType);
				}
				
				code.add(Label, subLabel);
				if (comparisonType == PrimitiveType.INTEGER
						|| comparisonType == PrimitiveType.BOOLEAN
						|| comparisonType == PrimitiveType.CHARACTER
						|| comparisonType == PrimitiveType.STRING) {
					code.add(Subtract);
				} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
					code.add(FSubtract);
				}
				
				if (comparisonType == PrimitiveType.INTEGER
						|| comparisonType == PrimitiveType.BOOLEAN
						|| comparisonType == PrimitiveType.CHARACTER
						|| comparisonType == PrimitiveType.STRING) {
					code.add(JumpFalse, falseLabel);
				} else if (comparisonType == PrimitiveType.FLOAT || comparisonType == PrimitiveType.RATIONAL) {
					code.add(JumpFZero, falseLabel);
				}
			} else {
				Type comparisonType = tempType;
				
				newValueCode(node);
				code.add(Label, startLabel);
				code.append(arg1.makeCopy());
				code.add(Label, arg2Label);
				code.append(arg2.makeCopy());
				
				code.add(Label, subLabel);
				if (comparisonType instanceof ArrayType) {
					code.add(Subtract);
				}
				
				if (comparisonType instanceof ArrayType) {
					code.add(JumpFalse, falseLabel);
				}
			}
			code.add(Jump, trueLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		
		private void generateTestDivideByZero(Type type) {
			assert(type instanceof PrimitiveType);
			PrimitiveType denomType = (PrimitiveType)type;
			code.add(Duplicate);
			if (denomType == PrimitiveType.INTEGER) {
				code.add(JumpFalse, RunTime.INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR);
			} else if (denomType == PrimitiveType.FLOAT) {
				code.add(JumpFZero, RunTime.FLOAT_DIVIDE_BY_ZERO_RUNTIME_ERROR);
			}
		}
		
		// [ ... maxSize index ]
		private void generateTestIndexOutOfBounds() {
			code.add(Duplicate);
			code.add(JumpNeg, RunTime.INDEX_OUT_OF_BOUNDS_ERROR);
			code.add(Subtract);
			code.add(Duplicate);
			code.add(JumpFalse, RunTime.INDEX_OUT_OF_BOUNDS_ERROR);
			code.add(JumpNeg, RunTime.INDEX_OUT_OF_BOUNDS_ERROR);
		}
		
		private void generateCastOpcode(Type castType, Type exprType) {
			//assert(castType instanceof PrimitiveType);
			if (castType instanceof PrimitiveType && exprType instanceof PrimitiveType) {
				PrimitiveType pCastType = (PrimitiveType)castType;
				//assert(exprType instanceof PrimitiveType);
				PrimitiveType pExprType = (PrimitiveType)exprType;
				if (pCastType == PrimitiveType.INTEGER && pExprType == PrimitiveType.FLOAT) {
					code.add(ConvertI);
				} else if (pCastType == PrimitiveType.FLOAT && (pExprType == PrimitiveType.INTEGER || pExprType == PrimitiveType.CHARACTER)) {
					code.add(ConvertF);
				} else if (pCastType == PrimitiveType.CHARACTER) {
					code.add(PushI, 127);
					code.add(BTAnd);
				} else if (pCastType == PrimitiveType.STRING) {
					
				} else if (pCastType == PrimitiveType.BOOLEAN && (pExprType == PrimitiveType.INTEGER || pExprType == PrimitiveType.CHARACTER)) {
					Labeller labeller = new Labeller("boolean-cast");
					
					String trueLabel  = labeller.newLabel("true");
					String falseLabel = labeller.newLabel("false");
					String joinLabel  = labeller.newLabel("join");
					
					code.add(JumpFalse, falseLabel);
					code.add(Jump, trueLabel);
					
					code.add(Label, trueLabel);
					code.add(PushI, 1);
					code.add(Jump, joinLabel);
					code.add(Label, falseLabel);
					code.add(PushI, 0);
					code.add(Jump, joinLabel);
					code.add(Label, joinLabel);
				} else if (pCastType == PrimitiveType.INTEGER && pExprType == PrimitiveType.RATIONAL) { // [ ... denominator numerator ]
					code.add(Exchange);
					generateTestDivideByZero(PrimitiveType.INTEGER);
					code.add(Divide);
				} else if (pCastType == PrimitiveType.FLOAT && pExprType == PrimitiveType.RATIONAL) { // [ ... denominator numerator ]
					code.add(ConvertF);
					code.add(Exchange);
					code.add(ConvertF);
					
					generateTestDivideByZero(PrimitiveType.FLOAT);
					code.add(FDivide);
				} else if (pCastType == PrimitiveType.RATIONAL && pExprType == PrimitiveType.INTEGER) {
					code.add(PushI, 1);
					code.add(Exchange);
				} else if (pCastType == PrimitiveType.RATIONAL && pExprType == PrimitiveType.CHARACTER) {
					code.add(PushI, 1);
					code.add(Exchange);
				} else if (pCastType == PrimitiveType.RATIONAL && pExprType == PrimitiveType.FLOAT) {
					code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
					code.add(PushI, 223092870);
					code.add(StoreI);
					code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
					code.add(LoadI);
					code.add(ConvertF);
					code.add(FMultiply);
					generateCastOpcode(PrimitiveType.INTEGER, PrimitiveType.FLOAT);
					
					code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
					code.add(Exchange);
					code.add(StoreI);
					
					code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
					code.add(LoadI);
					code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
					code.add(LoadI);
					code.add(Call, SubRoutines.FIND_GREATEST_COMMON_DENOMINATOR);
					
					code.add(Duplicate);
					code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
					code.add(Exchange);
					code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
					code.add(LoadI);
					code.add(Exchange);
					code.add(Divide);
					code.add(StoreI);
					code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
					code.add(Exchange);
					code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
					code.add(LoadI);
					code.add(Exchange);
					code.add(Divide);
					code.add(StoreI);
					
					code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
					code.add(LoadI);
					code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
					code.add(LoadI);
				}
			}
		}
		
		private void generateLambdaSetup(IdentifierNode dynamicLink, int frameSize) {
			ASMCodeFragment dynamicLinkFrag = removeAddressCode(dynamicLink);
			/*code.append(dynamicLinkFrag);
			code.add(PushD, RunTime.FRAME_POINTER);
			code.add(LoadI);
			code.add(opcodeForStore(dynamicLink.getType()));
			code.add(PushD, RunTime.FRAME_POINTER);
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(LoadI);
			code.add(StoreI);
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(LoadI);
			code.add(PushI, frameSize);
			code.add(Subtract);
			code.add(StoreI);*/
			
			
			code.add(PushD, RunTime.FRAME_POINTER);
			code.add(LoadI);
			
			code.add(PushD, RunTime.FRAME_POINTER);
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(LoadI);
			code.add(StoreI);
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(LoadI);
			code.add(PushI, frameSize);
			code.add(Subtract);
			code.add(StoreI);
			code.append(dynamicLinkFrag.makeCopy());
			code.add(Exchange);
			code.add(opcodeForStore(dynamicLink.getType()));
		}
		
		private void generateLambdaTeardown(IdentifierNode dynamicLink, int frameSize) {
			code.add(PushD, RunTime.FRAME_POINTER);
			ASMCodeFragment dynamicLinkFrag = removeValueCode(dynamicLink);
			code.append(dynamicLinkFrag.makeCopy());
			code.add(StoreI);
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(PushD, RunTime.STACK_POINTER);
			code.add(LoadI);
			code.add(PushI, frameSize);
			code.add(Add);
			code.add(StoreI);
		}
	}

}
