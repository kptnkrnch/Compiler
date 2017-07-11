package asmCodeGenerator.runtime;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import com.sun.javafx.binding.StringConstant;
import com.sun.org.apache.bcel.internal.classfile.Code;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
public class SubRoutines {
	//Subroutines
	public static final String RELEASE_REFERENCE_VARIABLE_SUBROUTINE = "$subrt-release-reference";
	public static final String PRINT_ARRAY_SUBROUTINE = "$subrt-print-array";
	public static final String LOAD_BASED_ON_SIZE_SUBROUTINE = "$subrt-load-based-on-size";
	public static final String LOAD_BASED_ON_TYPE_SUBROUTINE = "$subrt-load-based-on-type";
	public static final String FIND_GREATEST_COMMON_DENOMINATOR = "$subrt-find-gcd";
	public static final String ABSOLUTE_INTEGER_VALUE = "$subrt-abs-integer-value";
	public static final String COPY_BYTES_SUBROUTINE = "$subrt-copy-bytes";
	
	//Subroutine temp-variables
	public static final String RELEASE_TEMP_POINTER_1   = "$release-temp-pointer-1";
	public static final String RELEASE_TEMP_POINTER_2   = "$release-temp-pointer-2";
	public static final String RELEASE_COUNTER          = "$release-counter";
	public static final String PRINT_ARRAY_TEMP_POINTER_1   = "$print-array-temp-pointer-1";
	public static final String PRINT_ARRAY_TEMP_POINTER_2   = "$print-array-temp-pointer-2";
	public static final String PRINT_ARRAY_COUNTER          = "$print-array-counter";
	public static final String PRINT_ARRAY_ELEMENTS_COUNTER          = "$print-array-elements-counter";
	public static final String PRINT_ARRAY_TYPE_FORMAT      = "$print-array-format";
	public static final String LOADER_SIZE_VAR = "$loader-size-var";
	public static final String LOADER_TYPE_VAR = "$loader-type-var";
	public static final String LOADER_VALUE_VAR = "$loader-value-var";
	public static final String GCD_NUMERATOR = "$gcd-numerator";
	public static final String GCD_DENOMINATOR = "$gcd-denominator";
	public static final String GCD_TEMP = "$gcd-temp";
	public static final String ABS_INT_INPUT = "$abs-int-input";
	public static final String COPY_NUM_BYTES = "$copy-num-bytes";
	public static final String COPY_SRC_ADDR = "$copy-src-addr";
	public static final String COPY_DEST_ADDR = "$copy-dest-addr";
	public static final String COPY_RETURN_ADDR = "$copy-return-addr";
	public static final String COPY_COUNTER = "$copy-counter";

	private ASMCodeFragment globalSubroutineASM() {
		ASMCodeFragment result = new ASMCodeFragment(GENERATES_VOID);
		result.append(getSubroutineVariables());
		result.append(releaseReferenceVariable());
		result.append(printArray());
		result.append(loadBasedOnSize());
		result.append(loadBasedOnType());
		result.append(findGreatestCommonDenominator());
		result.append(absoluteIntegerValue());
		result.append(copyBytes());
		return result;
	}

	/* [ ... ArrayPointer ] */
	private ASMCodeFragment releaseReferenceVariable() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		Labeller releaseLabeller = new Labeller("release");
		String startRelease = releaseLabeller.newLabel("start");
		String endRelease = releaseLabeller.newLabel("end");
		String checkPermanentStatus = releaseLabeller.newLabel("check-permanent");
		String checkSubTypeIsReferenceStatus = releaseLabeller.newLabel("check-subtype");
		String checkDeleteStatus = releaseLabeller.newLabel("check-delete");
		
		String setDeleteStatus = releaseLabeller.newLabel("set-delete");
		
		String handleSubTypeIsReferenceStart = releaseLabeller.newLabel("handle-subtype-start");
		String handleSubTypeIsReferenceEnd = releaseLabeller.newLabel("handle-subtype-end");
		
		String releaseSubtypeLoopStart = releaseLabeller.newLabel("subtype-loop-start");
		String releaseSubtypeLoopEnd = releaseLabeller.newLabel("subtype-loop-end");
		
		frag.add(Label, RELEASE_REFERENCE_VARIABLE_SUBROUTINE);
		frag.add(Label, startRelease);
		frag.add(Exchange);
		frag.add(PushD, RELEASE_TEMP_POINTER_1);
		frag.add(Exchange);
		frag.add(StoreI);
		
		frag.add(PushD, RELEASE_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(PushI, ArrayType.statusOffset());
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, RELEASE_TEMP_POINTER_2);
		frag.add(Exchange);
		frag.add(StoreI);
		
		//check-permanent
		frag.add(Label, checkPermanentStatus);
		frag.add(PushD, RELEASE_TEMP_POINTER_2);
		frag.add(LoadI);
		frag.add(PushI, 8); //...1000
		frag.add(BTAnd);
		frag.add(JumpTrue, endRelease);
		
		//check-deleted
		frag.add(Label, checkDeleteStatus);
		frag.add(PushD, RELEASE_TEMP_POINTER_2);
		frag.add(LoadI);
		frag.add(PushI, 4); //...0100
		frag.add(BTAnd);
		frag.add(JumpTrue, endRelease);
		
		//check-if-subtype-is-a-reference-type
		frag.add(Label, checkSubTypeIsReferenceStatus);
		frag.add(PushD, RELEASE_TEMP_POINTER_2);
		frag.add(LoadI);
		frag.add(PushI, 2); //...0010
		frag.add(BTAnd);
		frag.add(JumpTrue, handleSubTypeIsReferenceStart);
		frag.add(Jump, setDeleteStatus);
		
		//Handle subtype is a reference type
		frag.add(Label, handleSubTypeIsReferenceStart);
		frag.add(PushD, RELEASE_COUNTER);
		frag.add(PushI, 0);
		frag.add(StoreI);
		
		frag.add(Label, releaseSubtypeLoopStart);
		frag.add(PushD, RELEASE_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(PushI, ArrayType.lengthOffset());
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, RELEASE_COUNTER);
		frag.add(LoadI);
		frag.add(Subtract);
		frag.add(JumpFalse, releaseSubtypeLoopEnd);
		frag.add(PushD, RELEASE_TEMP_POINTER_2);
		frag.add(LoadI);
		frag.add(PushD, RELEASE_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(Duplicate);
		
		frag.add(PushD, RELEASE_COUNTER);
		frag.add(LoadI);
		
		frag.add(Exchange);
		frag.add(Duplicate);
		frag.add(PushI, ArrayType.subtypeSizeOffset());
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, RELEASE_COUNTER);
		frag.add(LoadI);
		frag.add(Multiply);
		frag.add(PushI, ArrayType.headerSize());
		frag.add(Add);
		frag.add(Add);
		frag.add(LoadI);
		frag.add(Call, RELEASE_REFERENCE_VARIABLE_SUBROUTINE);
		
		frag.add(PushI, 1);
		frag.add(Add);
		frag.add(PushD, RELEASE_COUNTER);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(PushD, RELEASE_TEMP_POINTER_1);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(PushD, RELEASE_TEMP_POINTER_2);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(Jump, releaseSubtypeLoopStart);
		frag.add(Label, releaseSubtypeLoopEnd);
		frag.add(Label, handleSubTypeIsReferenceEnd);
		
		//set-delete-bit
		frag.add(Label, setDeleteStatus);
		frag.add(PushD, RELEASE_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(PushI, ArrayType.statusOffset());
		frag.add(Add);
		frag.add(PushD, RELEASE_TEMP_POINTER_2);
		frag.add(LoadI);
		frag.add(PushI, 4); //...0100
		frag.add(BTOr);
		frag.add(StoreI);
		
		//call MemoryManager deallocate
		frag.add(PushD, RELEASE_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);
		frag.add(Label, endRelease);
		frag.add(Return);
		
		return frag;
	}
	
	/* [... RootTypePrintFormatStringPointer ArrayPointer ] */
	private ASMCodeFragment printArray() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		Labeller printArrayLabeller = new Labeller("print-array");
		String startPrint = printArrayLabeller.newLabel("start");
		String endPrint = printArrayLabeller.newLabel("end");
		String checkSubTypeIsReferenceStatus = printArrayLabeller.newLabel("check-subtype");
		String checkDeleteStatus = printArrayLabeller.newLabel("check-delete");
		
		
		String handleSubTypeIsReferenceStart = printArrayLabeller.newLabel("handle-subtype-start");
		String handleSubTypeIsReferenceEnd = printArrayLabeller.newLabel("handle-subtype-end");
		
		String printSubtypeLoopStart = printArrayLabeller.newLabel("subtype-loop-start");
		String printSubtypeLoopEnd = printArrayLabeller.newLabel("subtype-loop-end");
		String printSubArrayLastElement = printArrayLabeller.newLabel("subtype-last-element");
		
		String printArrayElementsLoopStart = printArrayLabeller.newLabel("print-loop-start");
		String printArrayElementsLoopEnd = printArrayLabeller.newLabel("print-loop-end");
		String printArrayLastElement = printArrayLabeller.newLabel("last-element");
		
		String checkBoolean = printArrayLabeller.newLabel("check-boolean");
		String convertToBoolean = printArrayLabeller.newLabel("convert-boolean");
		String booleanTrue = printArrayLabeller.newLabel("boolean-true");
		String booleanJoin = printArrayLabeller.newLabel("boolean-join");
		
		String checkRational = printArrayLabeller.newLabel("check-rational");
		String printAsRational = printArrayLabeller.newLabel("convert-rational");
		String rationalJoin = printArrayLabeller.newLabel("rational-join");
		String printNormalType = printArrayLabeller.newLabel("print-normal-type");
		String checkDenominatorZero = printArrayLabeller.newLabel("rational-divide-zero");
		
		//rational printing labels
		String rationalRemainder = printArrayLabeller.newLabel("rational-remainder");
		String omitRationalElement1Start = printArrayLabeller.newLabel("start-omit-rat-element-1");
		String omitRationalElement1End = printArrayLabeller.newLabel("end-omit-rat-element-1");
		String omitRationalElement2Start = printArrayLabeller.newLabel("start-omit-rat-element-2");
		String omitRationalElement2End = printArrayLabeller.newLabel("end-omit-rat-element-2");
		String handleRemainderNegative = printArrayLabeller.newLabel("handle-rat-remainder-neg");
		
		frag.add(Label, PRINT_ARRAY_SUBROUTINE);
		frag.add(Label, startPrint);
		frag.add(Exchange);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(Exchange);
		frag.add(PushD, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(Exchange);
		frag.add(StoreI);
		
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(PushI, ArrayType.statusOffset());
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_2);
		frag.add(Exchange);
		frag.add(StoreI);
		
		//check-deleted
		frag.add(Label, checkDeleteStatus);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_2);
		frag.add(LoadI);
		frag.add(PushI, 4); //...0100
		frag.add(BTAnd);
		frag.add(JumpTrue, endPrint);
		
		frag.add(PushD, RunTime.L_BRACKET_PRINT_FORMAT);
		frag.add(Printf);
		
		//check-if-subtype-is-a-reference-type
		frag.add(Label, checkSubTypeIsReferenceStatus);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_2);
		frag.add(LoadI);
		frag.add(PushI, 2); //...0010
		frag.add(BTAnd);
		frag.add(JumpTrue, handleSubTypeIsReferenceStart);
		frag.add(Jump, printArrayElementsLoopStart);
		
		//Handle subtype is a reference type
		frag.add(Label, handleSubTypeIsReferenceStart);
		frag.add(PushD, PRINT_ARRAY_COUNTER);
		frag.add(PushI, 0);
		frag.add(StoreI);
		
		// Printing subArrays
		frag.add(Label, printSubtypeLoopStart);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(PushI, ArrayType.lengthOffset());
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_COUNTER);
		frag.add(LoadI);
		frag.add(Subtract);
		frag.add(JumpFalse, printSubtypeLoopEnd);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_2);
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(Duplicate);
		
		frag.add(PushD, PRINT_ARRAY_COUNTER);
		frag.add(LoadI);
		
		frag.add(Exchange);
		frag.add(Duplicate);
		frag.add(PushI, ArrayType.subtypeSizeOffset());
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_COUNTER);
		frag.add(LoadI);
		frag.add(Multiply);
		frag.add(PushI, ArrayType.headerSize());
		frag.add(Add);
		frag.add(Add);
		
		//loading input arguments for recursive subroutine call
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(LoadI);
		frag.add(Exchange);
		
		//Recursive call
		frag.add(Call, PRINT_ARRAY_SUBROUTINE);
		
		frag.add(PushI, 1);
		frag.add(Add);
		frag.add(PushD, PRINT_ARRAY_COUNTER);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_2);
		frag.add(Exchange);
		frag.add(StoreI);
		
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);//check if last value in array
		frag.add(LoadI);
		frag.add(PushI, ArrayType.lengthOffset()); 
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_COUNTER);
		frag.add(LoadI);
		frag.add(Subtract);
		frag.add(JumpFalse, printSubArrayLastElement);             //end of check
		frag.add(PushD, RunTime.COMMA_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(PushD, RunTime.SPACE_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Label, printSubArrayLastElement);
		
		frag.add(Jump, printSubtypeLoopStart);
		frag.add(Label, printSubtypeLoopEnd);
		frag.add(Label, handleSubTypeIsReferenceEnd);
		//End Print SubArrays
		
		//Start of Element printing
		frag.add(Jump, endPrint);
		frag.add(Label, printArrayElementsLoopStart);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(PushI, ArrayType.lengthOffset());
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_ELEMENTS_COUNTER);
		frag.add(LoadI);
		frag.add(Subtract);
		frag.add(JumpFalse, printArrayElementsLoopEnd);
		
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(Duplicate);
		frag.add(Duplicate);
		frag.add(PushI, ArrayType.subtypeSizeOffset());
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_ELEMENTS_COUNTER);
		frag.add(LoadI);
		frag.add(Multiply);
		frag.add(Add);
		frag.add(PushI, ArrayType.headerSize());
		frag.add(Add);
		/*frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(PushI, ArrayType.subtypeSizeOffset());
		frag.add(Add);
		frag.add(LoadI);*/
		frag.add(PushD, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(LoadI);
		frag.add(Exchange);
		frag.add(Call, LOAD_BASED_ON_TYPE_SUBROUTINE);

		/* Convert number to boolean if the print format is boolean print format */
		frag.add(PushD, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(LoadI);
		frag.add(PushD, RunTime.BOOLEAN_PRINT_FORMAT);
		frag.add(Subtract);
		frag.add(JumpFalse, convertToBoolean);
		frag.add(Jump, booleanJoin);
		frag.add(Label, convertToBoolean);
		frag.add(JumpTrue, booleanTrue);
		frag.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
		frag.add(Jump, booleanJoin);
		frag.add(Label, booleanTrue);
		frag.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
		frag.add(Label, booleanJoin);
		
		/* Convert to rational print format if the print format is the rational print format */
		frag.add(PushD, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(LoadI);
		frag.add(PushD, RunTime.RATIONAL_PRINT_FORMAT);
		frag.add(Subtract);
		frag.add(JumpFalse, printAsRational);
		frag.add(Jump, printNormalType);
		frag.add(Label, printAsRational);
		frag.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
		frag.add(Exchange);
		frag.add(StoreI);
		
		frag.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		frag.add(LoadI);
		frag.add(JumpFalse, RunTime.RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		
		frag.add(PushD, RunTime.RATIONAL_TEMP_VALUE_1);
		frag.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
		frag.add(LoadI);
		frag.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		frag.add(LoadI);
		frag.add(Divide);
		frag.add(StoreI);
		
		frag.add(PushD, RunTime.RATIONAL_TEMP_REMAINDER_1);
		frag.add(PushD, RunTime.RATIONAL_TEMP_NUMERATOR_1);
		frag.add(LoadI);
		frag.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		frag.add(LoadI);
		frag.add(Remainder);
		frag.add(StoreI);
		
		frag.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		frag.add(LoadI);
		frag.add(PushD, RunTime.RATIONAL_TEMP_REMAINDER_1);
		frag.add(LoadI);
		frag.add(PushD, RunTime.RATIONAL_TEMP_VALUE_1);
		frag.add(LoadI);
		
		frag.add(Duplicate);
		frag.add(JumpFalse, omitRationalElement1Start);
		frag.add(PushD, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(LoadI);
		frag.add(Printf);
		frag.add(Jump, omitRationalElement1End);
		
		frag.add(Label, omitRationalElement1Start);
		frag.add(Pop);
		frag.add(PushD, RunTime.RATIONAL_TEMP_DENOMINATOR_1);
		frag.add(LoadI);
		frag.add(PushD, RunTime.RATIONAL_TEMP_REMAINDER_1);
		frag.add(LoadI);
		frag.add(Multiply);
		frag.add(JumpNeg, handleRemainderNegative);
		frag.add(Label, omitRationalElement1End);
		
		frag.add(Label, rationalRemainder);
		frag.add(Duplicate);
		frag.add(JumpFalse, omitRationalElement2Start);
		frag.add(PushD, RunTime.UNDERSCORE_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Call, SubRoutines.ABSOLUTE_INTEGER_VALUE);
		frag.add(PushD, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(LoadI);
		frag.add(Printf);
		frag.add(PushD, RunTime.FORWARD_SLASH_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Call, SubRoutines.ABSOLUTE_INTEGER_VALUE);
		frag.add(PushD, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(LoadI);
		frag.add(Printf);
		frag.add(Jump, rationalJoin);
		
		frag.add(Label, handleRemainderNegative);
		frag.add(PushD, RunTime.MINUS_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Jump, rationalRemainder);
		
		frag.add(Label, omitRationalElement2Start);
		frag.add(Pop);
		frag.add(Pop);
		frag.add(Label, omitRationalElement2End);
		frag.add(Jump, rationalJoin);
		
		frag.add(Label, printNormalType);
		frag.add(PushD, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(LoadI);
		frag.add(Printf);
		frag.add(Label, rationalJoin);
		
		frag.add(PushD, PRINT_ARRAY_ELEMENTS_COUNTER); //incrementing the counter
		frag.add(LoadI);
		frag.add(PushI, 1);
		frag.add(Add);
		frag.add(PushD, PRINT_ARRAY_ELEMENTS_COUNTER);
		frag.add(Exchange);
		frag.add(StoreI);                             //end of counter increment
		frag.add(PushI, ArrayType.lengthOffset()); //check if last value in array
		frag.add(Add);
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_ELEMENTS_COUNTER);
		frag.add(LoadI);
		frag.add(Subtract);
		frag.add(JumpFalse, printArrayLastElement);             //end of check
		frag.add(PushD, RunTime.COMMA_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(PushD, RunTime.SPACE_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Label, printArrayLastElement);
		
		frag.add(PushD, PRINT_ARRAY_ELEMENTS_COUNTER);
		frag.add(LoadI);
		frag.add(PushD, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(LoadI);
		frag.add(Pop);
		frag.add(Pop);
		frag.add(Jump, printArrayElementsLoopStart);
		frag.add(Label, printArrayElementsLoopEnd);
		frag.add(PushD, PRINT_ARRAY_ELEMENTS_COUNTER);
		frag.add(PushI, 0);
		frag.add(StoreI);
		//end of elementPrinting
		
		frag.add(Label, endPrint);
		frag.add(PushD, RunTime.R_BRACKET_PRINT_FORMAT);
		frag.add(Printf);
		frag.add(Return);
		
		return frag;
	}
	
	/* [ ... ElementSizeInBytes PointerToElementToLoad ] */
	private ASMCodeFragment loadBasedOnSize() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		Labeller loaderLabeller = new Labeller("load-based-on-size");
		String start = loaderLabeller.newLabel("start-load");
		String end = loaderLabeller.newLabel("end-load");
		
		String load1Byte = loaderLabeller.newLabel("load-1");
		String load4Bytes = loaderLabeller.newLabel("load-4");
		String load8Bytes = loaderLabeller.newLabel("load-8");
		
		
		
		frag.add(Label, LOAD_BASED_ON_SIZE_SUBROUTINE);
		frag.add(Label, start);
		frag.add(Exchange);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(Exchange);
		frag.add(PushD, LOADER_SIZE_VAR);
		frag.add(Exchange);
		frag.add(StoreI);
		
		frag.add(PushD, LOADER_SIZE_VAR);
		frag.add(LoadI);
		frag.add(PushI, 1);
		frag.add(Subtract);
		frag.add(JumpFalse, load1Byte);
		
		frag.add(PushD, LOADER_SIZE_VAR);
		frag.add(LoadI);
		frag.add(PushI, 4);
		frag.add(Subtract);
		frag.add(JumpFalse, load4Bytes);
		
		frag.add(PushD, LOADER_SIZE_VAR);
		frag.add(LoadI);
		frag.add(PushI, 8);
		frag.add(Subtract);
		frag.add(JumpFalse, load8Bytes);
		frag.add(Jump, end);
		
		frag.add(Label, load1Byte);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(LoadC);
		frag.add(Exchange);
		frag.add(Jump, end);
		
		frag.add(Label, load4Bytes);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(LoadI);
		frag.add(Exchange);
		frag.add(Jump, end);
		
		frag.add(Label, load8Bytes);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(LoadF);
		frag.add(Exchange);
		frag.add(Jump, end);
		
		frag.add(Label, end);
		frag.add(Return);
		
		return frag;
	}
	
	/* [ ... PrintTypeFormat PointerToElementToLoad ] */
	private ASMCodeFragment loadBasedOnType() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		Labeller loaderLabeller = new Labeller("load-based-on-type");
		String start = loaderLabeller.newLabel("start-load");
		String end = loaderLabeller.newLabel("end-load");
		
		String loadAsBoolean = loaderLabeller.newLabel("load-boolean");
		String loadAsCharacter = loaderLabeller.newLabel("load-character");
		String loadAsFloat = loaderLabeller.newLabel("load-float");
		String loadAsInteger = loaderLabeller.newLabel("load-integer");
		String loadAsString = loaderLabeller.newLabel("load-string");
		String loadAsRational = loaderLabeller.newLabel("load-rational");
		
		frag.add(Label, LOAD_BASED_ON_TYPE_SUBROUTINE);
		frag.add(Label, start);
		frag.add(Exchange);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(Exchange);
		frag.add(PushD, LOADER_TYPE_VAR);
		frag.add(Exchange);
		frag.add(StoreI);
		
		frag.add(PushD, LOADER_TYPE_VAR);
		frag.add(LoadI);
		frag.add(PushD, RunTime.BOOLEAN_PRINT_FORMAT);
		frag.add(Subtract);
		frag.add(JumpFalse, loadAsBoolean);
		
		frag.add(PushD, LOADER_TYPE_VAR);
		frag.add(LoadI);
		frag.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
		frag.add(Subtract);
		frag.add(JumpFalse, loadAsCharacter);
		
		frag.add(PushD, LOADER_TYPE_VAR);
		frag.add(LoadI);
		frag.add(PushD, RunTime.FLOAT_PRINT_FORMAT);
		frag.add(Subtract);
		frag.add(JumpFalse, loadAsFloat);
		
		frag.add(PushD, LOADER_TYPE_VAR);
		frag.add(LoadI);
		frag.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
		frag.add(Subtract);
		frag.add(JumpFalse, loadAsInteger);
		
		frag.add(PushD, LOADER_TYPE_VAR);
		frag.add(LoadI);
		frag.add(PushD, RunTime.STRING_PRINT_FORMAT);
		frag.add(Subtract);
		frag.add(JumpFalse, loadAsString);
		
		frag.add(PushD, LOADER_TYPE_VAR);
		frag.add(LoadI);
		frag.add(PushD, RunTime.RATIONAL_PRINT_FORMAT);
		frag.add(Subtract);
		frag.add(JumpFalse, loadAsRational);
		
		frag.add(Label, loadAsBoolean);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(LoadC);
		frag.add(Exchange);
		frag.add(Jump, end);
		
		frag.add(Label, loadAsCharacter);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(LoadC);
		frag.add(Exchange);
		frag.add(Jump, end);
		
		frag.add(Label, loadAsFloat);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(LoadF);
		frag.add(Exchange);
		frag.add(Jump, end);
		
		frag.add(Label, loadAsInteger);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(LoadI);
		frag.add(Exchange);
		frag.add(Jump, end);
		
		frag.add(Label, loadAsString);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(LoadI);
		frag.add(PushI, PrimitiveType.STRING.getHeaderSize());
		frag.add(Add);
		frag.add(Exchange);
		frag.add(Jump, end);
		
		frag.add(Label, loadAsRational);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(LoadI);
		frag.add(Exchange);
		frag.add(PushD, LOADER_VALUE_VAR);
		frag.add(LoadI);
		frag.add(PushI, 4);
		frag.add(Add);
		frag.add(LoadI);
		frag.add(Exchange);
		frag.add(Jump, end);
		
		frag.add(Label, end);
		frag.add(Return);
		
		return frag;
	}
	
	/* [ ... ElementSizeInBytes PointerToElementToLoad ] */
	private ASMCodeFragment findGreatestCommonDenominator() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		Labeller loaderLabeller = new Labeller("find-gcd");
		String start = loaderLabeller.newLabel("start");
		String end = loaderLabeller.newLabel("end");
		
		String loopStart = loaderLabeller.newLabel("loop-start");
		String loopEnd = loaderLabeller.newLabel("loop-end");
		
		frag.add(Label, FIND_GREATEST_COMMON_DENOMINATOR);
		frag.add(Label, start);
		frag.add(Exchange);
		frag.add(PushD, GCD_NUMERATOR);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(Exchange);
		frag.add(PushD, GCD_DENOMINATOR);
		frag.add(Exchange);
		frag.add(StoreI);
		
		frag.add(PushD, GCD_DENOMINATOR);
		frag.add(LoadI);
		frag.add(JumpFalse, RunTime.INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		
		frag.add(PushD, GCD_NUMERATOR);
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(Call, ABSOLUTE_INTEGER_VALUE);
		frag.add(StoreI);
		frag.add(PushD, GCD_DENOMINATOR);
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(Call, ABSOLUTE_INTEGER_VALUE);
		frag.add(StoreI);
		
		frag.add(Label, loopStart);
		frag.add(PushD, GCD_NUMERATOR);
		frag.add(LoadI);
		frag.add(JumpNeg, end);
		frag.add(PushD, GCD_NUMERATOR);
		frag.add(LoadI);
		frag.add(JumpFalse, end);
		
		frag.add(PushD, GCD_TEMP);
		frag.add(PushD, GCD_NUMERATOR);
		frag.add(LoadI);
		frag.add(StoreI);
		
		frag.add(PushD, GCD_NUMERATOR);
		frag.add(PushD, GCD_DENOMINATOR);
		frag.add(LoadI);
		frag.add(PushD, GCD_NUMERATOR);
		frag.add(LoadI);
		frag.add(Remainder);
		frag.add(StoreI);
		
		frag.add(PushD, GCD_DENOMINATOR);
		frag.add(PushD, GCD_TEMP);
		frag.add(LoadI);
		frag.add(StoreI);
		
		frag.add(Jump, loopStart);
		frag.add(Label, loopEnd);
		
		frag.add(Label, end);
		frag.add(PushD, GCD_DENOMINATOR);
		frag.add(LoadI);
		frag.add(Exchange);
		frag.add(Return);
		
		return frag;
	}
	
	/* [ ... ElementSizeInBytes PointerToElementToLoad ] */
	private ASMCodeFragment absoluteIntegerValue() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		Labeller loaderLabeller = new Labeller("absolute-int");
		String start = loaderLabeller.newLabel("start");
		String end = loaderLabeller.newLabel("end");
		
		String makePositive = loaderLabeller.newLabel("make-positive");
		
		frag.add(Label, ABSOLUTE_INTEGER_VALUE);
		frag.add(Label, start);
		frag.add(Exchange);
		frag.add(PushD, ABS_INT_INPUT);
		frag.add(Exchange);
		frag.add(StoreI);
		
		frag.add(PushD, ABS_INT_INPUT);
		frag.add(LoadI);
		frag.add(JumpNeg, makePositive);
		frag.add(Jump, end);
		
		frag.add(Label, makePositive);
		frag.add(PushD, ABS_INT_INPUT);
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(PushI, -1);
		frag.add(Multiply);
		frag.add(StoreI);
		frag.add(Jump, end);
		
		frag.add(Label, end);
		frag.add(PushD, ABS_INT_INPUT);
		frag.add(LoadI);
		frag.add(Exchange);
		frag.add(Return);
		
		return frag;
	}
	
	/* [ ... sourceAddr destinationAddr numberOfBytes ] */
	private ASMCodeFragment copyBytes() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		Labeller loaderLabeller = new Labeller("copy-bytes");
		String start = loaderLabeller.newLabel("start");
		String end = loaderLabeller.newLabel("end");
		
		String makePositive = loaderLabeller.newLabel("make-positive");
		
		frag.add(Label, COPY_BYTES_SUBROUTINE);
		
		frag.add(PushD, COPY_RETURN_ADDR);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(PushD, COPY_NUM_BYTES);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(PushD, COPY_DEST_ADDR);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(PushD, COPY_SRC_ADDR);
		frag.add(Exchange);
		frag.add(StoreI);
		frag.add(PushD, COPY_COUNTER);
		frag.add(PushI, 0);
		frag.add(StoreI);
		
		frag.add(Label, start);
		frag.add(PushD, COPY_NUM_BYTES);
		frag.add(LoadI);
		frag.add(PushD, COPY_COUNTER);
		frag.add(LoadI);
		frag.add(Subtract);
		frag.add(JumpFalse, end);
		
		frag.add(PushD, COPY_DEST_ADDR);
		frag.add(LoadI);
		frag.add(PushD, COPY_COUNTER);
		frag.add(LoadI);
		frag.add(Add);
		frag.add(PushD, COPY_SRC_ADDR);
		frag.add(LoadI);
		frag.add(PushD, COPY_COUNTER);
		frag.add(LoadI);
		frag.add(Add);
		frag.add(LoadC);
		frag.add(StoreC);
		
		frag.add(PushD, COPY_COUNTER);
		frag.add(Duplicate);
		frag.add(LoadI);
		frag.add(PushI, 1);
		frag.add(Add);
		frag.add(StoreI);
		frag.add(Jump, start);
		
		frag.add(Label, end);
		frag.add(PushD, COPY_RETURN_ADDR);
		frag.add(LoadI);
		frag.add(Return);
		
		return frag;
	}
	
	private ASMCodeFragment getSubroutineVariables() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(DLabel, RELEASE_COUNTER);
		frag.add(DataI, 0);
		frag.add(DLabel, RELEASE_TEMP_POINTER_1);
		frag.add(DataI, 0);
		frag.add(DLabel, RELEASE_TEMP_POINTER_2);
		frag.add(DataI, 0);
		frag.add(DLabel, PRINT_ARRAY_COUNTER);
		frag.add(DataI, 0);
		frag.add(DLabel, PRINT_ARRAY_ELEMENTS_COUNTER);
		frag.add(DataI, 0);
		frag.add(DLabel, PRINT_ARRAY_TEMP_POINTER_1);
		frag.add(DataI, 0);
		frag.add(DLabel, PRINT_ARRAY_TEMP_POINTER_2);
		frag.add(DataI, 0);
		frag.add(DLabel, PRINT_ARRAY_TYPE_FORMAT);
		frag.add(DataI, 0);
		frag.add(DLabel, LOADER_SIZE_VAR);
		frag.add(DataI, 0);
		frag.add(DLabel, LOADER_TYPE_VAR);
		frag.add(DataI, 0);
		frag.add(DLabel, LOADER_VALUE_VAR);
		frag.add(DataI, 0);
		frag.add(DLabel, GCD_NUMERATOR);
		frag.add(DataI, 0);
		frag.add(DLabel, GCD_DENOMINATOR);
		frag.add(DataI, 0);
		frag.add(DLabel, GCD_TEMP);
		frag.add(DataI, 0);
		frag.add(DLabel, ABS_INT_INPUT);
		frag.add(DataI, 0);
		frag.add(DLabel, COPY_RETURN_ADDR);
		frag.add(DataI, 0);
		frag.add(DLabel, COPY_COUNTER);
		frag.add(DataI, 0);
		frag.add(DLabel, COPY_SRC_ADDR);
		frag.add(DataI, 0);
		frag.add(DLabel, COPY_DEST_ADDR);
		frag.add(DataI, 0);
		frag.add(DLabel, COPY_NUM_BYTES);
		frag.add(DataI, 0);
		
		return frag;
	}
	
	
	public static ASMCodeFragment getEnvironment() {
		SubRoutines subrt = new SubRoutines();
		return subrt.globalSubroutineASM();
	}
}
