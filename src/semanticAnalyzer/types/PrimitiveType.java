package semanticAnalyzer.types;


public enum PrimitiveType implements Type {
	BOOLEAN(1),
	INTEGER(4),
	FLOAT(8),
	STRING(4, 12, 8),
	CHARACTER(1),
	RATIONAL(8),
	ERROR(0),			// use as a value when a syntax error has occurred
	NO_TYPE(0, ""),
	ANY(0);		// use as a value when no type has been assigned.
	
	private int sizeInBytes;
	private int headerSizeInBytes;
	private String infoString;
	private int lengthOffset;
	
	private PrimitiveType(int size) {
		this.sizeInBytes = size;
		this.headerSizeInBytes = 0;
		this.infoString = toString();
		this.lengthOffset = 0;
	}
	private PrimitiveType(int size, int headerSize) {
		this.sizeInBytes = size;
		this.headerSizeInBytes = headerSize;
		this.infoString = toString();
	}
	private PrimitiveType(int size, int headerSize, int lengthOffset) {
		this.sizeInBytes = size;
		this.headerSizeInBytes = headerSize;
		this.infoString = toString();
		this.lengthOffset = lengthOffset;
	}
	private PrimitiveType(int size, String infoString) {
		this.sizeInBytes = size;
		this.headerSizeInBytes = 0;
		this.infoString = infoString;
	}
	public int getSize() {
		return sizeInBytes;
	}
	public String infoString() {
		return infoString;
	}
	@Override
	public int getHeaderSize() {
		// TODO Auto-generated method stub
		return headerSizeInBytes;
	}
	
	public int lengthOffset() {
		return this.lengthOffset;
	}
	
	@Override
	public boolean match(Type t) {
		if (t instanceof TypeVariable) {
			return t.match(this);
		} else {
			return t.equals(this);
		}
	}
	@Override
	public void setSize(int s) {
		this.sizeInBytes = s;
	}
}
