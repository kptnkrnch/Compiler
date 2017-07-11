package semanticAnalyzer.types;

public class VoidType implements Type {

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public int getHeaderSize() {
		return 0;
	}

	@Override
	public boolean match(Type t) {
		if (!(t instanceof VoidType)) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public String infoString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSize(int s) {
		// TODO Auto-generated method stub
		
	}

}
