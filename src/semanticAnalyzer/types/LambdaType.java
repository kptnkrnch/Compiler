package semanticAnalyzer.types;

import semanticAnalyzer.signatures.FunctionSignature;

public class LambdaType implements Type {
	private FunctionSignature signature;
	private int size;
	
	public LambdaType() {
		this.signature = null;
		this.size = 4;
	}
	
	public LambdaType(FunctionSignature signature) {
		this.signature = signature;
		this.size = 4;
	}
	
	public void setSignature(FunctionSignature signature) {
		this.signature = signature;
	}
	
	public FunctionSignature getSignature() {
		return this.signature;
	}
	
	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return this.size;
	}

	@Override
	public int getHeaderSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean match(Type t) {
		if (!(t instanceof LambdaType)) {
			return false;
		} else {
			return signature.compare(((LambdaType)t).getSignature());
		}
	}

	@Override
	public String infoString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSize(int s) {
		this.size = s;
	}

}
