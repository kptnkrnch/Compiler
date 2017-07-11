package symbolTable;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import inputHandler.TextLocation;
import lexicalAnalyzer.Keyword;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class Binding {
	private Type type;
	private TextLocation textLocation;
	private MemoryLocation memoryLocation;
	private String lexeme;
	private Boolean mutable;
	private Boolean isStatic;
	private Boolean isFunc;
	
	public Binding(Type type, TextLocation location, MemoryLocation memoryLocation, String lexeme, Boolean mutable) {
		super();
		this.type = type;
		this.textLocation = location;
		this.memoryLocation = memoryLocation;
		this.lexeme = lexeme;
		this.mutable = mutable;
		this.isStatic = false;
		this.isFunc = false;
	}
	
	public Binding(Type type, TextLocation location, MemoryLocation memoryLocation, String lexeme, Boolean mutable, Boolean isStatic, Boolean isFunc) {
		super();
		this.type = type;
		this.textLocation = location;
		this.memoryLocation = memoryLocation;
		this.lexeme = lexeme;
		this.mutable = mutable;
		this.isStatic = isStatic;
		this.isFunc = isFunc;
	}

	public String toString() {
		String declaredState;
		if (mutable) {
			declaredState = Keyword.VAR.getLexeme();
		} else {
			declaredState = Keyword.CONST.getLexeme();
		}
		return "[" + lexeme +
				" " + type +  // " " + textLocation +	
				" " + memoryLocation +
				" " + declaredState +
				"]";
	}	
	public String getLexeme() {
		return lexeme;
	}
	public Type getType() {
		return type;
	}
	public TextLocation getLocation() {
		return textLocation;
	}
	public MemoryLocation getMemoryLocation() {
		return memoryLocation;
	}
	public void setMemoryLocation(MemoryLocation location) {
		this.memoryLocation = location;
	}
	public Boolean getMutable() {
		return mutable;
	}
	public Boolean getStatic() {
		return isStatic;
	}
	public Boolean IsFunc() {
		return isFunc;
	}
	public void setIsFunction(boolean isFunc) {
		this.isFunc = isFunc;
	}
	public void generateAddress(ASMCodeFragment code) {
		memoryLocation.generateAddress(code, "%% " + lexeme);
	}
	
////////////////////////////////////////////////////////////////////////////////////
//Null Binding object
////////////////////////////////////////////////////////////////////////////////////

	public static Binding nullInstance() {
		return NullBinding.getInstance();
	}
	private static class NullBinding extends Binding {
		private static NullBinding instance=null;
		private NullBinding() {
			super(PrimitiveType.ERROR,
					TextLocation.nullInstance(),
					MemoryLocation.nullInstance(),
					"the-null-binding", false);
		}
		public static NullBinding getInstance() {
			if(instance==null)
				instance = new NullBinding();
			return instance;
		}
	}
}
