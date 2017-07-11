package optimizer;

public class BasicBlock implements Comparable<BasicBlock>{
	private int startIndex;
	private int endIndex;
	
	public BasicBlock(int start, int end) {
		this.startIndex = start;
		this.endIndex = end;
	}
	
	public void setStartIndex(int start) {
		this.startIndex = start;
	}
	
	public void setEndIndex(int end) {
		this.endIndex = end;
	}
	
	public int getStartIndex() {
		return this.startIndex;
	}
	
	public int getEndIndex() {
		return this.endIndex;
	}
	
	public boolean instructionIndexInBlock(int index) {
		return (index >= startIndex && index <= endIndex);
	}

	@Override
	public int compareTo(BasicBlock block) {
		return this.startIndex - block.startIndex;
	}
	
	public String toString() {
		String blockString = "[" + this.startIndex + ", " + this.endIndex + "]";
		return blockString;
	}
}
