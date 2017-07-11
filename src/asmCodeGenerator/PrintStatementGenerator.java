package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import parseTree.ParseNode;
import parseTree.nodeTypes.ArrayExpressionNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.TabNode;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import asmCodeGenerator.ASMCodeGenerator.CodeVisitor;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.runtime.RunTime;
import asmCodeGenerator.runtime.SubRoutines;

public class PrintStatementGenerator {
	ASMCodeFragment code;
	ASMCodeGenerator.CodeVisitor visitor;
	
	
	public PrintStatementGenerator(ASMCodeFragment code, CodeVisitor visitor) {
		super();
		this.code = code;
		this.visitor = visitor;
	}

	public void generate(PrintStatementNode node) {
		for(ParseNode child : node.getChildren()) {
			if(child instanceof NewlineNode || child instanceof SpaceNode || child instanceof TabNode) {
				ASMCodeFragment childCode = visitor.removeVoidCode(child);
				code.append(childCode);
			} else if (child.getType() instanceof ArrayType) {
				appendArrayPrintCode(child);
			} else if (child.getType() instanceof LambdaType) {
				code.add(PushD, RunTime.LAMBDA_PRINT_FORMAT);
				code.add(PushD, RunTime.STRING_PRINT_FORMAT);
				code.add(Printf);
			} else {
				appendPrintCode(child);
			}
		}
	}
	
	public void appendArrayPrintCode(ParseNode node) {
		Type type;
		if (node.getType() instanceof ArrayType) {
			type = (((ArrayType)(node.getType())).getRootType());
			//code.add(PushD, RunTime.L_BRACKET_PRINT_FORMAT);
			//code.add(Printf);
			if (type instanceof ArrayType) {
				
			} else {
				String format = printFormat(type);
				Labeller labeller = new Labeller("print-array");
				String startLabel = labeller.newLabel("start");
				String endLabel = labeller.newLabel("end");
				String lastElement = labeller.newLabel("last-element");
				
				code.add(PushD, format);
				code.append(visitor.removeValueCode(node));
				code.add(Call, SubRoutines.PRINT_ARRAY_SUBROUTINE);
				/*code.add(Label, startLabel);
				code.add(Duplicate);
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushD, RunTime.PRINT_ARRAY_COUNTER);
				code.add(LoadI);
				code.add(Subtract);
				code.add(JumpFalse, endLabel);
				code.add(Duplicate);
				code.add(Duplicate);
				code.add(PushI, ArrayType.subtypeSizeOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushD, RunTime.PRINT_ARRAY_COUNTER);
				code.add(LoadI);
				code.add(Multiply);
				code.add(Add);
				code.add(PushD, RunTime.PRINT_ARRAY_SIZE_OFFSET);
				code.add(LoadI);
				code.add(Add);
				code.add(LoadI);
				code.add(PushD, format);
				code.add(Printf);
				code.add(PushD, RunTime.PRINT_ARRAY_COUNTER); //incrementing the counter
				code.add(LoadI);
				code.add(PushI, 1);
				code.add(Add);
				code.add(PushD, RunTime.PRINT_ARRAY_COUNTER);
				code.add(Exchange);
				code.add(StoreI);                             //end of counter increment
				code.add(Duplicate);                          //check if last value in array
				code.add(PushI, ArrayType.lengthOffset());
				code.add(Add);
				code.add(LoadI);
				code.add(PushD, RunTime.PRINT_ARRAY_COUNTER);
				code.add(LoadI);
				code.add(Subtract);
				code.add(JumpFalse, lastElement);             //end of check
				code.add(PushD, RunTime.COMMA_PRINT_FORMAT);
				code.add(Printf);
				code.add(PushD, RunTime.SPACE_PRINT_FORMAT);
				code.add(Printf);
				code.add(Label, lastElement);
				code.add(Jump, startLabel);
				code.add(Label, endLabel);
				code.add(PushD, RunTime.PRINT_ARRAY_COUNTER);
				code.add(PushI, 0);
				code.add(StoreI);*/
			}
			//code.add(PushD, RunTime.R_BRACKET_PRINT_FORMAT);
			//code.add(Printf);
		}
	}

	private void appendPrintCode(ParseNode node) {
		String format = printFormat(node.getType());
		Labeller printLabeller = new Labeller("print-labeller");
		String rationalRemainder = printLabeller.newLabel("rational-remainder");
		String omitRationalElement1Start = printLabeller.newLabel("start-omit-rat-element-1");
		String omitRationalElement1End = printLabeller.newLabel("end-omit-rat-element-1");
		String omitRationalElement2Start = printLabeller.newLabel("start-omit-rat-element-2");
		String omitRationalElement2End = printLabeller.newLabel("end-omit-rat-element-2");
		String omittedRationalElement1Start = printLabeller.newLabel("start-omitted-rat-element-1");
		String omittedRationalElement1End = printLabeller.newLabel("end-omitted-rat-element-1");
		String handleRemainderNegative = printLabeller.newLabel("handle-rat-remainder-neg");

		code.append(visitor.removeValueCode(node));
		if (node.getTypeHeaderSize() > 0) {
			code.add(PushI, node.getTypeHeaderSize());
			code.add(Add);
		}
		convertToStringIfBoolean(node);
		
		convertToRationalIfRational(node);
		
		if (node.getType() == PrimitiveType.RATIONAL) {
			code.add(Duplicate);
			code.add(JumpFalse, omitRationalElement1Start);
			code.add(PushD, format);
			code.add(Printf);
			code.add(Jump, omitRationalElement1End);
			
			code.add(Label, omitRationalElement1Start);
			code.add(PushD, RunTime.PRINT_RATIONAL_OMITTED_ELEMENT_1);
			code.add(PushI, 1);
			code.add(StoreI);
			code.add(Pop);
			code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
			code.add(LoadI);
			code.add(PushD, RunTime.RATIONAL_TEMP_REMAINDER_1);
			code.add(LoadI);
			code.add(Multiply);
			code.add(JumpNeg, handleRemainderNegative);
			code.add(Label, omitRationalElement1End);
			
			code.add(Label, rationalRemainder);
			code.add(Duplicate);
			code.add(JumpFalse, omitRationalElement2Start);
			code.add(PushD, RunTime.UNDERSCORE_PRINT_FORMAT);
			code.add(Printf);
			code.add(Call, SubRoutines.ABSOLUTE_INTEGER_VALUE);
			code.add(PushD, format);
			code.add(Printf);
			code.add(PushD, RunTime.FORWARD_SLASH_PRINT_FORMAT);
			code.add(Printf);
			code.add(Call, SubRoutines.ABSOLUTE_INTEGER_VALUE);
			code.add(PushD, format);
			code.add(Printf);
			code.add(Jump, omitRationalElement2End);
			
			code.add(Label, handleRemainderNegative);
			code.add(PushD, RunTime.MINUS_PRINT_FORMAT);
			code.add(Printf);
			code.add(Jump, rationalRemainder);
			
			code.add(Label, omitRationalElement2Start);
			code.add(Pop);
			code.add(Pop);
			code.add(PushD, RunTime.PRINT_RATIONAL_OMITTED_ELEMENT_1);
			code.add(LoadI);
			code.add(JumpTrue, omittedRationalElement1Start);
			code.add(Label, omitRationalElement2End);
			
			code.add(Jump, omittedRationalElement1End);
			code.add(Label, omittedRationalElement1Start);
			code.add(PushI, 0);
			code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
			code.add(Printf);
			code.add(Label, omittedRationalElement1End);
			code.add(PushD, RunTime.PRINT_RATIONAL_OMITTED_ELEMENT_1);
			code.add(PushI, 0);
			code.add(StoreI);
		} else {
			code.add(PushD, format);
			code.add(Printf);
		}
	}
	private void convertToStringIfBoolean(ParseNode node) {
		if(node.getType() != PrimitiveType.BOOLEAN) {
			return;
		}
		
		Labeller labeller = new Labeller("print-boolean");
		String trueLabel = labeller.newLabel("true");
		String endLabel = labeller.newLabel("join");

		code.add(JumpTrue, trueLabel);
		code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
		code.add(Jump, endLabel);
		code.add(Label, trueLabel);
		code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
		code.add(Label, endLabel);
	}
	
	
	private void convertToRationalIfRational(ParseNode node) {
		if(node.getType() != PrimitiveType.RATIONAL) {
			return;
		}
		//code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
		code.add(Exchange);
		code.add(StoreI);
		//code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
		code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		code.add(Exchange);
		code.add(StoreI);
		
		code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		code.add(LoadI);
		code.add(JumpFalse, RunTime.RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		
		code.add(PushD, RunTime.RATIONAL_TEMP_VALUE_1);
		code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
		code.add(LoadI);
		code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		code.add(LoadI);
		code.add(Divide);
		code.add(StoreI);
		
		code.add(PushD, RunTime.RATIONAL_TEMP_REMAINDER_1);
		code.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
		code.add(LoadI);
		code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		code.add(LoadI);
		code.add(Remainder);
		code.add(StoreI);
		
		code.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		code.add(LoadI);
		code.add(PushD, RunTime.RATIONAL_TEMP_REMAINDER_1);
		code.add(LoadI);
		code.add(PushD, RunTime.RATIONAL_TEMP_VALUE_1);
		code.add(LoadI);
	}

	private static String printFormat(Type type) {
		assert type instanceof PrimitiveType;
		
		switch((PrimitiveType)type) {
		case INTEGER:	return RunTime.INTEGER_PRINT_FORMAT;
		case BOOLEAN:	return RunTime.BOOLEAN_PRINT_FORMAT;
		case FLOAT:     return RunTime.FLOAT_PRINT_FORMAT;
		case STRING:	return RunTime.STRING_PRINT_FORMAT;
		case CHARACTER: return RunTime.CHARACTER_PRINT_FORMAT;
		case RATIONAL:  return RunTime.RATIONAL_PRINT_FORMAT;
		default:		
			assert false : "Type " + type + " unimplemented in PrintStatementGenerator.printFormat()";
			return "";
		}
	}
}
