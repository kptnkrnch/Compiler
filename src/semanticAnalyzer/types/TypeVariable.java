package semanticAnalyzer.types;

public class TypeVariable implements Type {

	private Type type;
	private int size;
	
	public TypeVariable() {
		this.type = PrimitiveType.ANY;
		this.size = 0;
	}
	
	public TypeVariable(Type type) {
		this.type = type;
		this.size = type.getSize();
	}
	
	public boolean match(Type t) {
		if (type.equals(PrimitiveType.ANY)) {
			setType(t);
			return true;
		} else {
			return type.match(t);
		}
	}
	
	public void reset() {
		setType(PrimitiveType.ANY);
	}
	
	public Type getType() {
		return this.type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return this.size;
	}

	@Override
	public String infoString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getHeaderSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSize(int s) {
		this.size = s;
		
	}

}
