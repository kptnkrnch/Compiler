package asmCodeGenerator.runtime;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import semanticAnalyzer.types.ArrayType;
public class RunTime {
	public static final String EAT_LOCATION_ZERO      = "$eat-location-zero";		// helps us distinguish null pointers from real ones.
	public static final String INTEGER_PRINT_FORMAT   = "$print-format-integer";
	public static final String FLOAT_PRINT_FORMAT     = "$print-format-float";
	public static final String STRING_PRINT_FORMAT    = "$print-format-string";
	public static final String CHARACTER_PRINT_FORMAT = "$print-format-character";
	public static final String LAMBDA_PRINT_FORMAT    = "$print-format-lambda";
	public static final String RATIONAL_PRINT_FORMAT  = "$print-format-rational";
	public static final String BOOLEAN_PRINT_FORMAT   = "$print-format-boolean";
	public static final String NEWLINE_PRINT_FORMAT   = "$print-format-newline";
	public static final String TAB_PRINT_FORMAT       = "$print-format-tab";
	public static final String SPACE_PRINT_FORMAT     = "$print-format-space";
	public static final String COMMA_PRINT_FORMAT     = "$print-format-comma";
	public static final String UNDERSCORE_PRINT_FORMAT = "$print-format-underscore";
	public static final String FORWARD_SLASH_PRINT_FORMAT = "$print-format-fslash";
	public static final String MINUS_PRINT_FORMAT     = "$print-format-minus";
	public static final String L_BRACKET_PRINT_FORMAT = "$print-format-l-bracket";
	public static final String R_BRACKET_PRINT_FORMAT = "$print-format-r-bracket";
	public static final String PRINT_ARRAY_COUNTER    = "$print-array-counter";
	public static final String PRINT_ARRAY_SIZE_OFFSET = "$print-array-size-offset";
	public static final String BOOLEAN_TRUE_STRING    = "$boolean-true-string";
	public static final String BOOLEAN_FALSE_STRING   = "$boolean-false-string";
	public static final String GLOBAL_MEMORY_BLOCK    = "$global-memory-block";
	public static final String FRAME_POINTER	      = "$frame-pointer";
	public static final String STACK_POINTER	      = "$stack-pointer";
	public static final String USABLE_MEMORY_START    = "$usable-memory-start";
	public static final String MAIN_PROGRAM_LABEL     = "$$main";
	
	//Temp variables
	public static final String GLOBAL_TEMP_POINTER_1   = "$global-temp-pointer-1";
	public static final String GLOBAL_TEMP_POINTER_2   = "$global-temp-pointer-2";
	public static final String GLOBAL_TEMP_POINTER_3   = "$global-temp-pointer-3";
	public static final String GLOBAL_CLONE_POINTER   = "$global-clone-pointer";
	public static final String GLOBAL_TEMP_INTEGER_1   = "$global-temp-integer-1";
	public static final String GLOBAL_COUNTER          = "$global-counter";
	public static final String GLOBAL_COUNTER_2          = "$global-counter-2";
	public static final String RATIONAL_TEMP_NUMERATOR_1   = "$rational-temp-numerator-1";
	public static final String RATIONAL_TEMP_DENOMINATOR_1   = "$rational-temp-denominator-1";
	public static final String RATIONAL_TEMP_NUMERATOR_2   = "$rational-temp-numerator-2";
	public static final String RATIONAL_TEMP_DENOMINATOR_2   = "$rational-temp-denominator-2";
	public static final String RATIONAL_OPERATOR_NUMERATOR_1   = "$rational-oper-numerator-1";
	public static final String RATIONAL_OPERATOR_DENOMINATOR_1   = "$rational-oper-denominator-1";
	public static final String RATIONAL_OPERATOR_NUMERATOR_2   = "$rational-oper-numerator-2";
	public static final String RATIONAL_OPERATOR_DENOMINATOR_2   = "$rational-oper-denominator-2";
	public static final String RATIONAL_TEMP_REMAINDER_1   = "$rational-temp-remainder-1";
	public static final String RATIONAL_TEMP_VALUE_1   = "$rational-temp-value-1";
	public static final String PRINT_RATIONAL_OMITTED_ELEMENT_1   = "$print-rat-omitted-element-1";
	public static final String INDEX_TEMP_POINTER_1   = "$index-check-temp-pointer-1";
	
	public static final String MAP_ARRAY_EXPRESSION_POINTER = "$map-array-expression-pointer";
	public static final String FOLD_ARRAY_EXPRESSION_POINTER = "$fold-array-expression-pointer";
	public static final String REDUCE_ARRAY_EXPRESSION_POINTER = "$reduce-array-expression-pointer";
	public static final String ZIP_ARRAY_EXPRESSION_POINTER_1 = "$zip-array-expression-pointer-1";
	public static final String ZIP_ARRAY_EXPRESSION_POINTER_2 = "$zip-array-expression-pointer-2";
	public static final String REVERSE_EXPRESSION_POINTER = "$reverse-expression-pointer";
	
	//Runtime errors
	public static final String GENERAL_RUNTIME_ERROR = "$$general-runtime-error";
	public static final String INDEX_OUT_OF_BOUNDS_ERROR = "$$index-out-of-bounds";
	public static final String INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$i-divide-by-zero";
	public static final String FLOAT_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$f-divide-by-zero";
	public static final String RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$r-divide-by-zero";
	public static final String MISSING_RETURN_STATEMENT_ERROR = "$$missing-return-error";
	public static final String NEGATIVE_LENGTH_FOR_ARRAY_ERROR = "$$negative-array-length";
	public static final String ZIP_ARRAY_LENGTH_ERROR = "$$zip-array-length-error";
	public static final String FOLD_ARRAY_LENGTH_ERROR = "$$fold-array-length-error";

	private ASMCodeFragment environmentASM() {
		ASMCodeFragment result = new ASMCodeFragment(GENERATES_VOID);
		result.append(jumpToMain());
		result.append(stringsForPrintf());
		result.append(globalVariables());
		result.append(generateGlobalSubroutines());
		result.append(runtimeErrors());
		result.add(DLabel, USABLE_MEMORY_START);
		return result;
	}
	
	private ASMCodeFragment jumpToMain() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(Jump, MAIN_PROGRAM_LABEL);
		return frag;
	}

	private ASMCodeFragment stringsForPrintf() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(DLabel, EAT_LOCATION_ZERO);
		frag.add(DataZ, 8);
		frag.add(DLabel, INTEGER_PRINT_FORMAT);
		frag.add(DataS, "%d");
		frag.add(DLabel, FLOAT_PRINT_FORMAT);
		frag.add(DataS, "%g");
		frag.add(DLabel, BOOLEAN_PRINT_FORMAT);
		frag.add(DataS, "%s");
		frag.add(DLabel, STRING_PRINT_FORMAT);
		frag.add(DataS, "%s");
		frag.add(DLabel, CHARACTER_PRINT_FORMAT);
		frag.add(DataS, "%c");
		frag.add(DLabel, RATIONAL_PRINT_FORMAT);
		frag.add(DataS, "%d");
		frag.add(DLabel, NEWLINE_PRINT_FORMAT);
		frag.add(DataS, "\n");
		frag.add(DLabel, LAMBDA_PRINT_FORMAT);
		frag.add(DataS, "<lambda>");
		frag.add(DLabel, TAB_PRINT_FORMAT);
		frag.add(DataS, "\t");
		frag.add(DLabel, SPACE_PRINT_FORMAT);
		frag.add(DataS, " ");
		frag.add(DLabel, COMMA_PRINT_FORMAT);
		frag.add(DataS, ",");
		frag.add(DLabel, UNDERSCORE_PRINT_FORMAT);
		frag.add(DataS, "_");
		frag.add(DLabel, FORWARD_SLASH_PRINT_FORMAT);
		frag.add(DataS, "/");
		frag.add(DLabel, MINUS_PRINT_FORMAT);
		frag.add(DataS, "-");
		frag.add(DLabel, L_BRACKET_PRINT_FORMAT);
		frag.add(DataS, "[");
		frag.add(DLabel, R_BRACKET_PRINT_FORMAT);
		frag.add(DataS, "]");
		frag.add(DLabel, BOOLEAN_TRUE_STRING);
		frag.add(DataS, "true");
		frag.add(DLabel, BOOLEAN_FALSE_STRING);
		frag.add(DataS, "false");
		
		return frag;
	}
	
	private ASMCodeFragment globalVariables() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(DLabel, GLOBAL_COUNTER);
		frag.add(DataI, 0);
		frag.add(DLabel, GLOBAL_COUNTER_2);
		frag.add(DataI, 0);
		frag.add(DLabel, GLOBAL_TEMP_POINTER_1);
		frag.add(DataI, 0);
		frag.add(DLabel, GLOBAL_TEMP_POINTER_2);
		frag.add(DataI, 0);
		frag.add(DLabel, GLOBAL_TEMP_POINTER_3);
		frag.add(DataI, 0);
		frag.add(DLabel, GLOBAL_CLONE_POINTER);
		frag.add(DataI, 0);
		frag.add(DLabel, GLOBAL_TEMP_INTEGER_1);
		frag.add(DataI, 0);
		
		frag.add(DLabel, RATIONAL_TEMP_NUMERATOR_1);
		frag.add(DataI, 0);
		frag.add(DLabel, RATIONAL_TEMP_DENOMINATOR_1);
		frag.add(DataI, 0);
		frag.add(DLabel, RATIONAL_TEMP_NUMERATOR_2);
		frag.add(DataI, 0);
		frag.add(DLabel, RATIONAL_TEMP_DENOMINATOR_2);
		frag.add(DataI, 0);
		frag.add(DLabel, RATIONAL_OPERATOR_NUMERATOR_1);
		frag.add(DataI, 0);
		frag.add(DLabel, RATIONAL_OPERATOR_DENOMINATOR_1);
		frag.add(DataI, 0);
		frag.add(DLabel, RATIONAL_OPERATOR_NUMERATOR_2);
		frag.add(DataI, 0);
		frag.add(DLabel, RATIONAL_OPERATOR_DENOMINATOR_2);
		frag.add(DataI, 0);
		frag.add(DLabel, RATIONAL_TEMP_REMAINDER_1);
		frag.add(DataI, 0);
		frag.add(DLabel, RATIONAL_TEMP_VALUE_1);
		frag.add(DataI, 0);
		frag.add(DLabel, PRINT_RATIONAL_OMITTED_ELEMENT_1);
		frag.add(DataI, 0);
		
		frag.add(DLabel, MAP_ARRAY_EXPRESSION_POINTER);
		frag.add(DataI, 0);
		frag.add(DLabel, FOLD_ARRAY_EXPRESSION_POINTER);
		frag.add(DataI, 0);
		frag.add(DLabel, REDUCE_ARRAY_EXPRESSION_POINTER);
		frag.add(DataI, 0);
		frag.add(DLabel, ZIP_ARRAY_EXPRESSION_POINTER_1);
		frag.add(DataI, 0);
		frag.add(DLabel, ZIP_ARRAY_EXPRESSION_POINTER_2);
		frag.add(DataI, 0);
		frag.add(DLabel, REVERSE_EXPRESSION_POINTER);
		frag.add(DataI, 0);
		
		frag.add(DLabel, INDEX_TEMP_POINTER_1);
		frag.add(DataI, 0);
		
		return frag;
	}
	
	private ASMCodeFragment generateGlobalSubroutines() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		return frag;
	}
	
	private ASMCodeFragment runtimeErrors() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		
		generalRuntimeError(frag);
		integerDivideByZeroError(frag);
		floatDivideByZeroError(frag);
		rationalDivideByZeroError(frag);
		indexOutOfBoundsError(frag);
		missingReturnStatementError(frag);
		negativeArrayLengthError(frag);
		zipArrayLengthError(frag);
		foldArrayLengthError(frag);
		
		return frag;
	}
	private ASMCodeFragment generalRuntimeError(ASMCodeFragment frag) {
		String generalErrorMessage = "$errors-general-message";

		frag.add(DLabel, generalErrorMessage);
		frag.add(DataS, "Runtime error: %s\n");
		
		frag.add(Label, GENERAL_RUNTIME_ERROR);
		frag.add(PushD, generalErrorMessage);
		frag.add(Printf);
		frag.add(Halt);
		return frag;
	}
	private void integerDivideByZeroError(ASMCodeFragment frag) {
		String intDivideByZeroMessage = "$errors-int-divide-by-zero";
		
		frag.add(DLabel, intDivideByZeroMessage);
		frag.add(DataS, "integer divide by zero");
		
		frag.add(Label, INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, intDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	private void floatDivideByZeroError(ASMCodeFragment frag) {
		String floatDivideByZeroMessage = "$errors-float-divide-by-zero";
		
		frag.add(DLabel, floatDivideByZeroMessage);
		frag.add(DataS, "float divide by zero");
		
		frag.add(Label, FLOAT_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, floatDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	private void rationalDivideByZeroError(ASMCodeFragment frag) {
		String rationalDivideByZeroMessage = "$errors-rational-divide-by-zero";
		
		frag.add(DLabel, rationalDivideByZeroMessage);
		frag.add(DataS, "rational denominator zero");
		
		frag.add(Label, RATIONAL_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, rationalDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	
	private void indexOutOfBoundsError(ASMCodeFragment frag) {
		
		String indexOutOfBoundsMessage = "$errors-index-out-of-bounds";
		
		frag.add(DLabel, indexOutOfBoundsMessage);
		frag.add(DataS, "index out of bounds");
		
		frag.add(Label, INDEX_OUT_OF_BOUNDS_ERROR);
		frag.add(PushD, indexOutOfBoundsMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	
	private void missingReturnStatementError(ASMCodeFragment frag) {
		String missingReturnStatementMessage = "$errors-missing-return";
		
		frag.add(DLabel, missingReturnStatementMessage);
		frag.add(DataS, "missing return statement");
		
		frag.add(Label, MISSING_RETURN_STATEMENT_ERROR);
		frag.add(PushD, missingReturnStatementMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	
	private void negativeArrayLengthError(ASMCodeFragment frag) {
		String negativeArrayLengthMessage = "$errors-negative-array-length";
		
		frag.add(DLabel, negativeArrayLengthMessage);
		frag.add(DataS, "negative length given for array");
		
		frag.add(Label, NEGATIVE_LENGTH_FOR_ARRAY_ERROR);
		frag.add(PushD, negativeArrayLengthMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	
	private void zipArrayLengthError(ASMCodeFragment frag) {
		String zipArrayLengthMessage = "$errors-zip-array-length";
		
		frag.add(DLabel, zipArrayLengthMessage);
		frag.add(DataS, "arrays have differing lengths");
		
		frag.add(Label, ZIP_ARRAY_LENGTH_ERROR);
		frag.add(PushD, zipArrayLengthMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	
	private void foldArrayLengthError(ASMCodeFragment frag) {
		String foldArrayLengthMessage = "$errors-fold-array-length";
		
		frag.add(DLabel, foldArrayLengthMessage);
		frag.add(DataS, "invalid array length for fold; array length cannot be zero");
		
		frag.add(Label, FOLD_ARRAY_LENGTH_ERROR);
		frag.add(PushD, foldArrayLengthMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	
	public static ASMCodeFragment getEnvironment() {
		RunTime rt = new RunTime();
		return rt.environmentASM();
	}
}
