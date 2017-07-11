package semanticAnalyzer.types;

public class ArrayType implements Type {
	private Type subType;
	private Type rootPromotedFromType;
	private int size;
	
	//HEADER INFORMATION
	private int typeIdentifier;
	private int status;
	private int subtypeSize;
	private int length;
	
	public ArrayType(Type subtype) {
		this.rootPromotedFromType = PrimitiveType.NO_TYPE;
		this.subType = subtype;
		this.typeIdentifier = 7;
		this.status = 2;
		this.length = 0;
		this.size = 4;
	}
	
	public static ArrayType make(Type subType) {
		return new ArrayType(subType);
	}
	
	public void setSubType(Type type) {
		subType = type;
		subtypeSize = type.getSize();
	}
	
	public Type getSubType() {
		return subType;
	}
	
	
	public Type getRootType() {
		if (subType instanceof ArrayType) {
			return ((ArrayType)subType).getRootType();
		} else {
			return subType;
		}
	}
	
	public void setRootType(Type type) {
		if (subType instanceof ArrayType) {
			((ArrayType)subType).setRootType(type);
		} else if (subType != null){
			subType = type;
		}
	}
	
	public void promoteRootType(Type type) {
		if (subType instanceof ArrayType) {
			((ArrayType)subType).setRootType(type);
		} else if (subType != null){
			rootPromotedFromType = subType; 
			subType = type;
		}
	}
	
	public Type getRootPromotedFromType(Type type) {
		return rootPromotedFromType;
	}
	
	public int getSubtypeSize() {
		return subType.getSize();
	}
	
	@Override
	public int getSize() {
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
		return 16;
	}
	
	public static int headerSize() {
		return 16;
	}
	
	public static int typeIdentifierOffset() {
		return 0;
	}
	
	public static int statusOffset() {
		return 4;
	}
	
	public static int subtypeSizeOffset() {
		return 8;
	}
	
	public static int lengthOffset() {
		return 12;
	}

	@Override
	public boolean match(Type t) {
		if (!(t instanceof ArrayType)) {
			return false;
		} else {
			return subType.match(((ArrayType)t).getSubType());
		}
	}

	@Override
	public void setSize(int size) {
		this.size = size;
	}

}
