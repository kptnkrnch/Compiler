package semanticAnalyzer.signatures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Punctuator;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.LambdaType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.TypeVariable;
import tokens.CastToken;
import tokens.IndexOperatorToken;
import tokens.SubstringOperatorToken;


public class FunctionSignatures extends ArrayList<FunctionSignature> {
	private static final long serialVersionUID = -4907792488209670697L;
	private static Map<Object, FunctionSignatures> signaturesForKey = new HashMap<Object, FunctionSignatures>();
	
	Object key;
	
	public FunctionSignatures(Object key, FunctionSignature ...functionSignatures) {
		this.key = key;
		for(FunctionSignature functionSignature: functionSignatures) {
			add(functionSignature);
		}
		signaturesForKey.put(key, this);
	}
	
	public Object getKey() {
		return key;
	}
	public boolean hasKey(Object key) {
		return this.key.equals(key);
	}
	
	public FunctionSignature acceptingSignature(List<Type> types) {
		for(FunctionSignature functionSignature: this) {
			t.reset();
			u.reset();
			s.reset();
			if(functionSignature.accepts(types)) {
				return functionSignature;
			}
		}
		return FunctionSignature.nullInstance();
	}
	public boolean accepts(List<Type> types) {
		return !acceptingSignature(types).isNull();
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	// access to FunctionSignatures by key object.
	
	public static FunctionSignatures nullSignatures = new FunctionSignatures(0, FunctionSignature.nullInstance());

	public static FunctionSignatures signaturesOf(Object key) {
		if(signaturesForKey.containsKey(key)) {
			return signaturesForKey.get(key);
		}
		return nullSignatures;
	}
	public static FunctionSignature signature(Object key, List<Type> types) {
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(key);
		return signatures.acceptingSignature(types);
	}

	public static FunctionSignature addSignature(Object key, List<Type> types) {
		FunctionSignatures signatures = signaturesForKey.get(key);
		FunctionSignature signature = new FunctionSignature(1, (Type[])types.toArray());
		signatures.add(0, signature);
		return signature;
	}
	
	public static FunctionSignature addSignature(Object key, Object whichVariant, List<Type> types) {
		FunctionSignatures signatures = signaturesForKey.get(key);
		FunctionSignature signature = new FunctionSignature(whichVariant, (Type[])types.toArray());
		signatures.add(0, signature);
		return signature;
	}
	
	public List<Type> getArgumentNumberTypes(int argumentNumber) {
		ArrayList<Type> types = new ArrayList<Type>();
		for(FunctionSignature functionSignature: this) {
			types.add(functionSignature.getArgument(argumentNumber));
		}
		return types;
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	// Put the signatures for operators in the following static block.
	
	public static TypeVariable t = new TypeVariable();
	public static TypeVariable u = new TypeVariable();
	public static TypeVariable s = new TypeVariable();
	
	static {
		// here's one example to get you started with FunctionSignatures: the signatures for addition.		
		// for this to work, you should statically import PrimitiveType.*

//		new FunctionSignatures(Punctuator.ADD,
//		    new FunctionSignature(ASMOpcode.Add, INTEGER, INTEGER, INTEGER),
//		    new FunctionSignature(ASMOpcode.FAdd, FLOAT, FLOAT, FLOAT)
//		);
		
		// First, we use the operator itself (in this case the Punctuator ADD) as the key.
		// Then, we give that key two signatures: one an (INT x INT -> INT) and the other
		// a (FLOAT x FLOAT -> FLOAT).  Each signature has a "whichVariant" parameter where
		// I'm placing the instruction (ASMOpcode) that needs to be executed.
		//
		// I'll follow the convention that if a signature has an ASMOpcode for its whichVariant,
		// then to generate code for the operation, one only needs to generate the code for
		// the operands (in order) and then add to that the Opcode.  For instance, the code for
		// floating addition should look like:
		//
		//		(generate argument 1)	: may be many instructions
		//		(generate argument 2)   : ditto
		//		FAdd					: just one instruction
		//
		// If the code that an operator should generate is more complicated than this, then
		// I will not use an ASMOpcode for the whichVariant.  In these cases I typically use
		// a small object with one method (the "Command" design pattern) that generates the
		// required code.
		
		//Arithmetic operators
		new FunctionSignatures(Punctuator.ADD,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL),
			new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.STRING),
			new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.CHARACTER, PrimitiveType.STRING),
			new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.STRING, PrimitiveType.STRING)
		);
		new FunctionSignatures(Punctuator.SUBTRACT,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);
		new FunctionSignatures(Punctuator.MULTIPLY,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);
		new FunctionSignatures(Punctuator.DIVIDE,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);
		
		//Assignment operator
		new FunctionSignatures(Punctuator.ASSIGN,
			new FunctionSignature(1, t, t, t)
			/*new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
			new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER),
			new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.STRING)*/
		);
		
		//Comparison operators
		new FunctionSignatures(Punctuator.EQUAL,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, new ArrayType(t), new ArrayType(t), PrimitiveType.BOOLEAN)//,
			//new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOLEAN) //Removed as per a spec change (Email notification)
		);
		new FunctionSignatures(Punctuator.LESSER,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.GREATER,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.GREATER_EQUAL,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.LESSER_EQUAL,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.NOT_EQUAL,
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN),
			new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),//,
			new FunctionSignature(1, new ArrayType(t), new ArrayType(t), PrimitiveType.BOOLEAN)
			//new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.BOOLEAN) //Removed as per a spec change (Email notification)
		);
		
		// CASTING
		new FunctionSignatures(CastToken.getOperator(),
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.CHARACTER, PrimitiveType.INTEGER), // casting character to integer
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.FLOAT, PrimitiveType.INTEGER), // casting float to integer
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.FLOAT), // casting integer to float
			new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.INTEGER, PrimitiveType.CHARACTER), // casting integer to character
			new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN), // casting integer to boolean
			new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN), // casting character to boolean
			new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.RATIONAL, PrimitiveType.FLOAT), // casting rational to float
			new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.RATIONAL, PrimitiveType.INTEGER), // casting rational to integer
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.INTEGER, PrimitiveType.RATIONAL), // casting integer to rational
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.CHARACTER, PrimitiveType.RATIONAL), // casting character to rational
			new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.FLOAT, PrimitiveType.RATIONAL), // casting float to rational
			// casting to same type
			new FunctionSignature(1, t, t, t)//,
			//new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
			//new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER),
			//new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
			//new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.STRING)
		);
		
		//If Statement
		new FunctionSignatures(Keyword.IF,
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
		//While Statement
		new FunctionSignatures(Keyword.WHILE,
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
		//And/Or/Not operators
		new FunctionSignatures(Punctuator.AND,
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.OR,
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.NOT,
				new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
		//Length Statement
		new FunctionSignatures(Keyword.LENGTH,
				new FunctionSignature(1, new ArrayType(t), PrimitiveType.INTEGER),
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.INTEGER)
		);
		//Index Operator
		new FunctionSignatures(IndexOperatorToken.getOperator(),
				new FunctionSignature(1, new ArrayType(t), PrimitiveType.INTEGER, t),
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.INTEGER, PrimitiveType.CHARACTER)
		);
		//Substring Operator
		new FunctionSignatures(SubstringOperatorToken.getOperator(),
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.STRING)
		);
		//Clone Statement
		new FunctionSignatures(Keyword.CLONE,
				new FunctionSignature(1, new ArrayType(t), new ArrayType(t))
		);
		//Array Creation Statement
		new FunctionSignatures(Keyword.NEW,
				new FunctionSignature(1, new ArrayType(t), new ArrayType(t))
		);
		//Array Creation Statement
		new FunctionSignatures(Keyword.RELEASE,
				new FunctionSignature(1, new ArrayType(t), new ArrayType(t))
		);
		//Array/String Reverse Statement
		new FunctionSignatures(Keyword.REVERSE,
				new FunctionSignature(1, new ArrayType(t), new ArrayType(t)),
				new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING)
		);
		
		//Rational Operators
		//Over Operator
		new FunctionSignatures(Punctuator.OVER,
				new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.RATIONAL)
		);
		//Express Over Operator
		new FunctionSignatures(Punctuator.EXPRESS_OVER,
				//new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER), //for testing promotions
				new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
				new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.INTEGER)
		);
		//Rationalize Operator
		new FunctionSignatures(Punctuator.RATIONALIZE,
				new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.INTEGER, PrimitiveType.RATIONAL),
				new FunctionSignature(1, PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.RATIONAL)
		);
		
		//Lambda Related Operators
		//Map Statement
		new FunctionSignatures(Keyword.MAP,
				new FunctionSignature(1, new ArrayType(t), new LambdaType(new FunctionSignature(1, t, u)), new ArrayType(u))
		);
		//Reduce Statement
		new FunctionSignatures(Keyword.REDUCE,
				new FunctionSignature(1, new ArrayType(t), new LambdaType(new FunctionSignature(1, t, PrimitiveType.BOOLEAN)), new ArrayType(t))
		);
		//Fold Statement
		new FunctionSignatures(Keyword.FOLD,
				new FunctionSignature(1, new ArrayType(t), new LambdaType(new FunctionSignature(1, t, t, t)), t),
				new FunctionSignature(1, new ArrayType(t), u, new LambdaType(new FunctionSignature(1, u, t, t)), u)
		);
		//Zip Statement
		new FunctionSignatures(Keyword.ZIP,
				new FunctionSignature(1, new ArrayType(s), new ArrayType(t), new LambdaType(new FunctionSignature(1, s, t, u)), new ArrayType(u))
		);
		
	}

}
