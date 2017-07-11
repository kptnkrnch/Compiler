package semanticAnalyzer.signatures;

import java.util.List;

import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.TypeVariable;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;

//immutable
public class FunctionSignature {
	private static final boolean ALL_TYPES_ACCEPT_ERROR_TYPES = true;
	private Type resultType;
	private Type[] paramTypes;
	private Type[] allTypes;
	Object whichVariant;
	
	
	///////////////////////////////////////////////////////////////
	// construction
	
	public FunctionSignature(Object whichVariant, Type ...types) {
		assert(types.length >= 1);
		allTypes = types;
		storeParamTypes(types);
		resultType = allTypes[allTypes.length-1];
		this.whichVariant = whichVariant;
	}
	public FunctionSignature(Object whichVariant, List<Type> types) {
		assert(types.size() >= 1);
		allTypes = (Type[]) types.toArray(new Type[0]);
		storeParamTypes((Type[]) types.toArray(new Type[0]));
		resultType = allTypes[allTypes.length-1];
		this.whichVariant = whichVariant;
	}
	private void storeParamTypes(Type[] types) {
		paramTypes = new Type[types.length-1];
		for(int i=0; i<types.length-1; i++) {
			paramTypes[i] = types[i];
		}
	}
	
	
	
	///////////////////////////////////////////////////////////////
	// accessors
	
	public Object getVariant() {
		return whichVariant;
	}
	public Type resultType() {
		if (resultType instanceof TypeVariable) {
			Type returnType = ((TypeVariable) resultType).getType();
			Type current = returnType;
			while (current instanceof TypeVariable) {
				Type temp = ((TypeVariable) current).getType();
				current = temp;
			}
			return returnType;
		} else if (resultType instanceof ArrayType) {
			if (((ArrayType) resultType).getSubType() instanceof TypeVariable) {
				Type returnType = new ArrayType(((TypeVariable)(((ArrayType) resultType).getSubType())).getType());
				return returnType;
			}
		}
		return resultType;
	}
	public boolean isNull() {
		return false;
	}
	
	public Type getArgument(int argumentNumber) {
		if (argumentNumber >= 0 && argumentNumber < this.paramTypes.length) {
			Type temp = this.paramTypes[argumentNumber];
			if (resultType instanceof TypeVariable) {
				Type returnType = ((TypeVariable) resultType).getType();
				Type current = returnType;
				while (current instanceof TypeVariable) {
					Type tVarTemp = ((TypeVariable) current).getType();
					current = tVarTemp;
				}
				return returnType;
			}
			return temp;
		} else {
			return null;
		}
	}
	
	public Type[] getAllTypes() {
		return this.allTypes;
	}
	
	///////////////////////////////////////////////////////////////
	// main query

	public boolean accepts(List<Type> types) {
		if(types.size() != paramTypes.length) {
			return false;
		}
		
		for(int i=0; i<paramTypes.length; i++) {
			if(!assignableTo(paramTypes[i], types.get(i))) {
				return false;
			}
		}		
		return true;
	}
	private boolean assignableTo(Type variableType, Type valueType) {
		if(valueType == PrimitiveType.ERROR && ALL_TYPES_ACCEPT_ERROR_TYPES) {
			return true;
		}
		if (variableType instanceof TypeVariable) {
			return ((TypeVariable)variableType).match(valueType);
		} else if (variableType instanceof ArrayType) {
			return ((ArrayType)variableType).match(valueType);
		} else if (variableType instanceof LambdaType) {
			return ((LambdaType)variableType).match(valueType);
		}
		return variableType.equals(valueType);
	}
	
	// Null object pattern
	private static FunctionSignature neverMatchedSignature = new FunctionSignature(1, PrimitiveType.ERROR) {
		public boolean accepts(List<Type> types) {
			return false;
		}
		public boolean isNull() {
			return true;
		}
	};
	public static FunctionSignature nullInstance() {
		return neverMatchedSignature;
	}
	
	///////////////////////////////////////////////////////////////////
	// Signatures for pika-0 operators
	// this section will probably disappear in pika-1 (in favor of FunctionSignatures)
	
	private static FunctionSignature assignSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature addSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature subtractSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature multiplySignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature divideSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature fAssignSignature = new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT);
	private static FunctionSignature fAddSignature = new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT);
	private static FunctionSignature fSubtractSignature = new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT);
	private static FunctionSignature fMultiplySignature = new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT);
	private static FunctionSignature fDivideSignature = new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT);
	private static FunctionSignature greaterSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature lesserSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature iEqualSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature fGreaterSignature = new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN);
	private static FunctionSignature fLesserSignature = new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN);


	
	// the switch here is ugly compared to polymorphism.  This should perhaps be a method on Lextant.
	public static FunctionSignature signatureOf(Lextant lextant, List<Type> types) {
		Type type1 = types.get(0);
		Type type2 = types.get(1);
		assert(lextant instanceof Punctuator);
		assert(type1 instanceof PrimitiveType);
		assert(type2 instanceof PrimitiveType);
		Punctuator punctuator = (Punctuator)lextant;
		PrimitiveType primitive1 = (PrimitiveType)type1;
		PrimitiveType primitive2 = (PrimitiveType)type1;
		if (primitive1 == PrimitiveType.INTEGER && primitive2 == PrimitiveType.INTEGER) {
			switch(punctuator) {
			case ASSIGN:    return assignSignature;
			case ADD:		return addSignature;
			case SUBTRACT:  return subtractSignature;
			case MULTIPLY:	return multiplySignature;
			case DIVIDE:    return divideSignature;
			case GREATER:	return greaterSignature;
			case LESSER:    return lesserSignature;
			case EQUAL:     return iEqualSignature;
	
			default:
				return neverMatchedSignature;
			}
		} else if (primitive1 == PrimitiveType.FLOAT && primitive2 == PrimitiveType.FLOAT) {
			switch(punctuator) {
			case ASSIGN:    return fAssignSignature;
			case ADD:		return fAddSignature;
			case SUBTRACT:  return fSubtractSignature;
			case MULTIPLY:	return fMultiplySignature;
			case DIVIDE:    return fDivideSignature;
			case GREATER:	return fGreaterSignature;
			case LESSER:    return fLesserSignature;
	
			default:
				return neverMatchedSignature;
			}
		} else {
			return neverMatchedSignature;
		}
	}
	
	public boolean compare(FunctionSignature signature) {
		Type[] signatureTypes = signature.getAllTypes();
		if (allTypes.length != signatureTypes.length) {
			return false;
		}
		for (int i = 0; i < signatureTypes.length; i++) {
			Type temp1 = allTypes[i];
			Type temp2 = signatureTypes[i];
			if (!temp1.match(temp2)) {
				return false;
			}
		}
		return true;
	}

}